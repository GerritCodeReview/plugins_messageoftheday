// Copyright (C) 2016 The Android Open Source Project
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

import static com.google.gerrit.server.config.ConfigResource.CONFIG_KIND;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.CapabilityDefinition;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;

class Module extends AbstractModule {
  private static final String GIT_REPOSITORY = "gitRepository";
  private final PluginConfigFactory cfg;
  private final String pluginName;

  @Inject
  Module(PluginConfigFactory cfg, @PluginName String pluginName) {
    this.cfg = cfg;
    this.pluginName = pluginName;
  }

  @Override
  protected void configure() {
    bind(CapabilityDefinition.class)
        .annotatedWith(Exports.named(UpdateBannerCapability.NAME))
        .to(UpdateBannerCapability.class);
    install(
        new RestApiModule() {
          @Override
          protected void configure() {
            get(CONFIG_KIND, "message").to(GetMessage.class);
            post(CONFIG_KIND, "message").to(SetMessage.class);
          }
        });
    String configGit = cfg.getFromGerritConfig(pluginName).getString(GIT_REPOSITORY);
    if (configGit != null) {
      install(new GitBasedModule(configGit));
    } else {
      install(new FileBasedModule());
    }
  }
}
