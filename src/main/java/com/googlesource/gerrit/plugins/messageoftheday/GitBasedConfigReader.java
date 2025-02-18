// Copyright (C) 2025 The Android Open Source Project
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

import static com.googlesource.gerrit.plugins.messageoftheday.GitBasedMessageStore.MASTER;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.restapi.BinaryResult;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.server.cache.CacheModule;
import com.google.gerrit.server.change.FileContentUtil;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

@Singleton
public class GitBasedConfigReader implements GitReferenceUpdatedListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";

  private static final String CONFIG = "config";

  public static Module module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(CONFIG, String.class, CachedConfig.class).loader(Loader.class);
      }
    };
  }

  private final LoadingCache<String, CachedConfig> cache;
  private final Project.NameKey configRepoName;

  @Inject
  GitBasedConfigReader(
      @Named(CONFIG) LoadingCache<String, CachedConfig> cache,
      @GitConfigRepo Project.NameKey configRepoName) {
    this.cache = cache;
    this.configRepoName = configRepoName;
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    if (!event.getProjectName().equals(configRepoName.get())) {
      return;
    }
    if (!event.getRefName().equals(MASTER)) {
      return;
    }
    cache.invalidateAll();
  }

  public CachedConfig getConfig() {
    try {
      return cache.get(CONFIG);
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e).log("Couldn't load plugin config");
      return CachedConfig.empty();
    }
  }

  private static class Loader extends CacheLoader<String, CachedConfig> {
    private final NameKey configRepoName;
    private final Repository configRepo;
    private final ProjectCache projectCache;
    private final FileContentUtil fileContentUtil;

    @Inject
    Loader(
        @GitConfigRepo Project.NameKey configRepoName,
        @GitConfigRepo Repository configRepo,
        ProjectCache projectCache,
        FileContentUtil fileContentUtil) {
      this.configRepoName = configRepoName;
      this.configRepo = configRepo;
      this.projectCache = projectCache;
      this.fileContentUtil = fileContentUtil;
    }

    @Override
    public CachedConfig load(String key) throws Exception {
      Optional<ProjectState> state = projectCache.get(configRepoName);
      if (state.isEmpty()) {
        return CachedConfig.empty();
      }
      Ref ref = configRepo.exactRef(MASTER);
      if (ref == null) {
        return CachedConfig.empty();
      }

      ObjectId commitId = ref.getObjectId();

      try {
        Config config = new Config();
        BinaryResult content =
            fileContentUtil.getContent(configRepo, state.get(), commitId, "messageoftheday.config");
        config.fromText(content.asString());

        String id = config.getString(SECTION_MESSAGE, null, KEY_ID);
        BinaryResult result =
            fileContentUtil.getContent(configRepo, state.get(), commitId, id + ".html");
        String message = result.asString();
        return CachedConfig.create(config, message, commitId);
      } catch (ResourceNotFoundException e) {
        logger.atWarning().withCause(e).log("Loading failed");
        return CachedConfig.empty();
      }
    }
  }
}
