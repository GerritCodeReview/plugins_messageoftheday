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

import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.File;
import java.nio.file.Path;

public class FileBasedModule extends AbstractModule {
  private static final String CONFIG_DIR = "configDir";
  private static final String DATA_DIR = "dataDir";

  @Override
  protected void configure() {
    bind(MessageStore.class).to(FileBasedMessageStore.class);
  }

  @Provides
  @Singleton
  @ConfigFile
  File provideConfigFile(
      PluginConfigFactory cfg, SitePaths sitePaths, @PluginName String pluginName) {
    String configDir = cfg.getFromGerritConfig(pluginName).getString(CONFIG_DIR);
    return (configDir != null ? Path.of(configDir) : sitePaths.etc_dir)
        .resolve(pluginName + ".config")
        .toFile();
  }

  @Provides
  @Singleton
  @DataDir
  Path provideDataDir(
      PluginConfigFactory cfg, @PluginName String pluginName, @PluginData Path dataDirPath) {
    String dataDir = cfg.getFromGerritConfig(pluginName).getString(DATA_DIR);
    return dataDir != null ? Path.of(dataDir) : dataDirPath;
  }
}
