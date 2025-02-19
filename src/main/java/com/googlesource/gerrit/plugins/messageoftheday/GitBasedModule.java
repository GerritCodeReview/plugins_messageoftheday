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

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;

public class GitBasedModule extends LifecycleModule {
  private final String configRepoName;

  GitBasedModule(String configRepoName) {
    this.configRepoName = configRepoName;
  }

  @Override
  protected void configure() {
    bind(MessageStore.class).to(GitBasedMessageStore.class);
    bind(Project.NameKey.class)
        .annotatedWith(GitConfigRepo.class)
        .toInstance(Project.nameKey(configRepoName));
    listener().to(CloseConfigRepo.class);
    install(GitBasedConfigReader.module());
    DynamicSet.bind(binder(), GitReferenceUpdatedListener.class).to(GitBasedConfigReader.class);
  }

  @Provides
  @Singleton
  @GitConfigRepo
  Repository getConfigRepo(GitRepositoryManager repoManager, @GitConfigRepo Project.NameKey name)
      throws RepositoryNotFoundException, IOException {
    return repoManager.openRepository(name);
  }
}
