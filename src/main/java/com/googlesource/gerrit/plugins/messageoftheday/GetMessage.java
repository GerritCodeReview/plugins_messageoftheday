// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.messageoftheday;

import static com.google.gerrit.server.project.ProjectCache.illegalState;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.change.FileContentUtil;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.meta.VersionedMetaData;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetMessage implements RestReadView<ConfigResource> {
  protected static class VersionedMotdConfig extends VersionedMetaData {
    protected final BranchNameKey branch;
    protected final String fileName;
    protected Config cfg;

    public VersionedMotdConfig(BranchNameKey branch, String fileName) {
      this.branch = branch;
      this.fileName = fileName;
    }

    @Override
    protected String getRefName() {
      return branch.branch();
    }

    @Override
    protected void onLoad() throws IOException, ConfigInvalidException {
      cfg = readConfig(fileName);
    }

    @Override
    protected boolean onSave(CommitBuilder commit) throws IOException, ConfigInvalidException {
      return false;
    }

    public Config get() {
      if (cfg == null) {
        cfg = new Config();
      }
      return cfg;
    }
  }

  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_STARTS_AT = "startsAt";
  private static final String KEY_EXPIRES_AT = "expiresAt";

  private static final String SECTION_PROJECT = "project";
  private static final String KEY_NAME = "name";
  private static final String KEY_BRANCH = "branch";
  private static final String KEY_CFG_DIR = "configDir";
  private static final String KEY_DATA_DIR = "dataDir";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmm");

  private static final Logger log = LoggerFactory.getLogger(GetMessage.class);

  private final GitRepositoryManager repoManager;
  private final Config gerritCfg;
  private final String pluginName;
  private final SitePaths sitePaths;
  private final Path dataDirPath;
  private final String pluginCfgName;
  private final ProjectCache projectCache;
  private final FileContentUtil fileContentUtil;

  private Project.NameKey customProject;
  private ObjectId customBranchObjId;
  private volatile Config cfg;

  @Inject
  public GetMessage(
      GitRepositoryManager repoManager,
      @GerritServerConfig Config gerritCfg,
      @PluginName String pluginName,
      @PluginData Path dataDirPath,
      SitePaths sitePaths,
      ProjectCache projectCache,
      FileContentUtil fileContentUtil) {
    this.repoManager = repoManager;
    this.gerritCfg = gerritCfg;
    this.pluginName = pluginName;
    this.dataDirPath = dataDirPath;
    this.sitePaths = sitePaths;
    this.projectCache = projectCache;
    this.fileContentUtil = fileContentUtil;
    this.pluginCfgName = pluginName + ".config";
  }

  @Override
  public Response<MessageOfTheDayInfo> apply(ConfigResource rsrc) {
    MessageOfTheDayInfo motd = new MessageOfTheDayInfo();

    String projectName = gerritCfg.getString(pluginName, SECTION_PROJECT, KEY_NAME);
    if (projectName != null) {
      customProject = Project.NameKey.parse(projectName);
    }

    cfg = getPluginCfg();
    if (cfg == null) {
      log.warn(String.format("could not load %s, no message will be shown", pluginCfgName));
      return Response.none();
    }

    String htmlFileId = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(htmlFileId)) {
      log.warn("id not defined, no message will be shown");
      return Response.none();
    }

    try {
      motd.expiresAt = DATE_FORMAT.parse(cfg.getString(SECTION_MESSAGE, null, KEY_EXPIRES_AT));
    } catch (ParseException | NullPointerException e) {
      log.warn("expiresAt not defined, no message will be shown");
      return Response.none();
    }

    try {
      String startsAt = cfg.getString(SECTION_MESSAGE, null, KEY_STARTS_AT);
      motd.startsAt = Strings.isNullOrEmpty(startsAt) ? new Date() : DATE_FORMAT.parse(startsAt);
    } catch (ParseException e) {
      motd.startsAt = new Date();
    }

    if (motd.startsAt.compareTo(new Date()) > 0 || motd.expiresAt.compareTo(new Date()) < 0) {
      log.debug("Current date/time is outside of the startsAt..expiresAt interval");
      return Response.none();
    }

    try {
      motd.html = getHtmlContent(htmlFileId + ".html");
    } catch (IOException | ResourceNotFoundException | BadRequestException e) {
      log.warn(
          String.format(
              "No HTML-file was found for message %s, no message will be shown", htmlFileId));
      return Response.none();
    }

    motd.id = Integer.toString(motd.html.hashCode());
    return Response.ok(motd);
  }

  private Config getPluginCfg() {
    return customProject != null ? getPluginCfgFromCustomProject() : getPluginCfgFromSite();
  }

  private String getHtmlContent(String file)
      throws BadRequestException, IOException, ResourceNotFoundException {
    return customProject != null ? getHtmlFromCustomProject(file) : getHtmlFromSite(file);
  }

  private Config getPluginCfgFromCustomProject() {
    String cfg = pluginCfgName;
    String cfgDir = gerritCfg.getString(pluginName, SECTION_PROJECT, KEY_CFG_DIR);
    if (cfgDir != null) {
      cfg = Path.of(cfgDir).resolve(pluginCfgName).toFile().getPath();
    }
    BranchNameKey branch =
        BranchNameKey.create(
            customProject,
            MoreObjects.firstNonNull(
                gerritCfg.getString(pluginName, SECTION_PROJECT, KEY_BRANCH), "refs/heads/master"));
    VersionedMotdConfig versionedCfg = new VersionedMotdConfig(branch, cfg);
    try (Repository repository = repoManager.openRepository(customProject)) {
      customBranchObjId = repository.exactRef(branch.branch()).getObjectId();
      versionedCfg.load(customProject, repository, customBranchObjId);
      return versionedCfg.cfg;
    } catch (IOException | ConfigInvalidException e) {
      log.warn(
          String.format("Unable to load config from custom project %s", customProject.get()), e);
    }
    return null;
  }

  private Config getPluginCfgFromSite() {
    FileBasedConfig cfg =
        new FileBasedConfig(sitePaths.etc_dir.resolve(pluginCfgName).toFile(), FS.DETECTED);
    try {
      cfg.load();
    } catch (ConfigInvalidException | IOException e) {
      return null;
    }
    return cfg;
  }

  private String getHtmlFromCustomProject(String file)
      throws BadRequestException, IOException, ResourceNotFoundException {
    String dataDir = gerritCfg.getString(pluginName, SECTION_PROJECT, KEY_DATA_DIR);
    if (dataDir != null) {
      file = Path.of(dataDir).resolve(file).toFile().getPath();
    }
    return fileContentUtil
        .getContent(
            projectCache.get(customProject).orElseThrow(illegalState(customProject)),
            customBranchObjId,
            file,
            null)
        .asString();
  }

  private String getHtmlFromSite(String file) throws IOException {
    return Files.readString(dataDirPath.resolve(file));
  }
}
