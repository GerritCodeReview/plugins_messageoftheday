// Copyright (C) 2024 The Android Open Source Project
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

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gerrit.server.cache.CacheModule;
import com.google.gerrit.server.change.FileContentUtil;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutionException;

public class HtmlMessageCacheImpl implements HtmlMessageCache {
  protected static final String CACHE_NAME = "htmlmsg_by_commit";

  public static CacheModule module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(CACHE_NAME, FileNameKey.class, String.class).maximumWeight(1).loader(Loader.class);
        bind(HtmlMessageCacheImpl.class);
        bind(HtmlMessageCache.class).to(HtmlMessageCacheImpl.class);
      }
    };
  }

  protected final LoadingCache<FileNameKey, String> cache;

  @Inject
  protected HtmlMessageCacheImpl(@Named(CACHE_NAME) LoadingCache<FileNameKey, String> cache) {
    this.cache = cache;
  }

  @Override
  public String getHtmlMsg(FileNameKey file) throws ExecutionException {
    return cache.get(file);
  }

  public static class Loader extends CacheLoader<FileNameKey, String> {
    private final ProjectCache projectCache;
    private final FileContentUtil fileContentUtil;

    @Inject
    public Loader(ProjectCache projectCache, FileContentUtil fileContentUtil) {
      this.projectCache = projectCache;
      this.fileContentUtil = fileContentUtil;
    }

    @Override
    public String load(FileNameKey file) throws Exception {
      // TODO: force show the msg when we load it from cache, eben if someone dismisses it?
      return fileContentUtil
          .getContent(
              projectCache.get(file.project()).orElseThrow(illegalState(file.project())),
              file.rev(),
              file.file(),
              null)
          .asString();
    }
  }
}
