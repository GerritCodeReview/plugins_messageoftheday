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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.meta.VersionedMetaData;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
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
  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_STARTS_AT = "startsAt";
  private static final String KEY_EXPIRES_AT = "expiresAt";
  private static final String SECTION_PROJECT = "project";
  private static final String KEY_NAME = "name";
  private static final String KEY_BRANCH = "branch";
  private static final String KEY_CFG = "config";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmm");

  private static final Logger log = LoggerFactory.getLogger(GetMessage.class);

  private final GitRepositoryManager repoManager;
  private final Config gerritCfg;
  private final String pluginName;
  private final SitePaths sitePaths;
  private final Path dataDirPath;
  private final String pluginCfgName;
  private final HtmlMessageCache htmlMessageCache;

  private Project.NameKey project;
  private ObjectId branchObjId;
  private volatile Config mtodCfg;

  @Inject
  public GetMessage(
      GitRepositoryManager repoManager,
      @GerritServerConfig Config gerritCfg,
      @PluginName String pluginName,
      @PluginData Path dataDirPath,
      SitePaths sitePaths,
      HtmlMessageCache htmlMessageCache) {
    this.repoManager = repoManager;
    this.gerritCfg = gerritCfg;
    this.pluginName = pluginName;
    this.dataDirPath = dataDirPath;
    this.sitePaths = sitePaths;
    this.htmlMessageCache = htmlMessageCache;
    this.pluginCfgName = pluginName + ".config";
  }

  @Override
  public Response<MessageOfTheDayInfo> apply(ConfigResource rsrc) {
    MessageOfTheDayInfo motd = new MessageOfTheDayInfo();

    loadFromProject();

    if (mtodCfg == null) {
      FileBasedConfig dataDirMtodCfg =
          new FileBasedConfig(sitePaths.etc_dir.resolve(pluginCfgName).toFile(), FS.DETECTED);
      try {
        dataDirMtodCfg.load();
      } catch (ConfigInvalidException | IOException e) {
        return null;
      }
      mtodCfg = dataDirMtodCfg;
    }

    motd.id = mtodCfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(motd.id)) {
      log.warn("id not defined, no message will be shown");
      return Response.none();
    }

    try {
      motd.expiresAt = DATE_FORMAT.parse(mtodCfg.getString(SECTION_MESSAGE, null, KEY_EXPIRES_AT));
    } catch (ParseException | NullPointerException e) {
      log.warn("expiresAt not defined, no message will be shown");
      return Response.none();
    }

    try {
      String startsAt = mtodCfg.getString(SECTION_MESSAGE, null, KEY_STARTS_AT);
      motd.startsAt = Strings.isNullOrEmpty(startsAt) ? new Date() : DATE_FORMAT.parse(startsAt);
    } catch (ParseException e) {
      motd.startsAt = new Date();
    }

    if (motd.startsAt.compareTo(new Date()) > 0 || motd.expiresAt.compareTo(new Date()) < 0) {
      log.debug("Current date/time is outside of the startsAt..expiresAt interval");
      return Response.none();
    }

    try {
      if (project != null && branchObjId != null) {
        motd.html =
            htmlMessageCache.getHtmlMsg(
                FileNameKey.create(project, branchObjId, motd.id + ".html"));
      } else {
        motd.html = Files.readString(dataDirPath.resolve(motd.id + ".html"));
      }
    } catch (IOException | ExecutionException e) {
      log.warn(
          String.format(
              "No HTML-file was found for message %s, no message will be shown", motd.id));
      return Response.none();
    }
    return Response.ok(motd);
  }

  private void loadFromProject() {
    String projectName = gerritCfg.getString(pluginName, SECTION_PROJECT, KEY_NAME);
    String pluginCfg =
        MoreObjects.firstNonNull(
            gerritCfg.getString(pluginName, SECTION_PROJECT, KEY_CFG), pluginCfgName);
    if (projectName != null) {
      project = Project.NameKey.parse(projectName);
      BranchNameKey branch =
          BranchNameKey.create(
              project,
              MoreObjects.firstNonNull(
                  gerritCfg.getString(pluginName, SECTION_PROJECT, KEY_BRANCH),
                  "refs/heads/master"));
      VersionedMotdConfig versionedMtodCfg = new VersionedMotdConfig(branch, pluginCfg);
      try (Repository repository = repoManager.openRepository(project)) {
        branchObjId = repository.exactRef(branch.branch()).getObjectId();
        versionedMtodCfg.load(project, repository, branchObjId);
        mtodCfg = versionedMtodCfg.cfg;
      } catch (IOException | ConfigInvalidException e) {
        log.warn(
            String.format(
                "Unable to use project %s config. Falling back to data dir. " + "Reason: %s",
                projectName, e.getMessage()));
      }
    }
  }

  static class VersionedMotdConfig extends VersionedMetaData {
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
}
