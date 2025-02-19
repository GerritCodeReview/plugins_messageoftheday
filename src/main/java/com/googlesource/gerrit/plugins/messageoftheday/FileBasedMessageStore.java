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

import static com.google.common.io.Files.asCharSink;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.CharSink;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

@Singleton
public class FileBasedMessageStore implements MessageStore {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";

  private final File cfgFile;
  private final Path dataDir;

  @Inject
  FileBasedMessageStore(@ConfigFile File cfgFile, @DataDir Path dataDir) {
    this.cfgFile = cfgFile;
    this.dataDir = dataDir;
  }

  @Override
  public ConfiguredMessage getConfiguredMessage() throws MessageStoreException {
    FileBasedConfig cfg = loadConfig();

    String htmlFileId = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(htmlFileId)) {
      logger.atWarning().log("id not defined, no message will be shown");
      return ConfiguredMessage.create(cfg, null);
    }

    String message = loadMessage(htmlFileId);
    return ConfiguredMessage.create(cfg, message);
  }

  private FileBasedConfig loadConfig() throws MessageStoreException {
    FileBasedConfig cfg = new FileBasedConfig(cfgFile, FS.DETECTED);
    try {
      cfg.load();
    } catch (ConfigInvalidException | IOException e) {
      throw new MessageStoreException("plugin cfg is invalid or could not be loaded", e);
    }
    return cfg;
  }

  private String loadMessage(String id) {
    try {
      return new String(Files.readAllBytes(dataDir.resolve(id + ".html")), UTF_8);
    } catch (IOException e1) {
      logger.atWarning().log("No HTML-file was found for message %s, no message will be shown", id);
      return null;
    }
  }

  @Override
  public void saveConfiguredMessage(ConfiguredMessage message) throws MessageStoreException {
    FileBasedConfig configFile = new FileBasedConfig(message.config(), cfgFile, FS.DETECTED);

    addAll(configFile, message.config());

    String id = configFile.getString(SECTION_MESSAGE, null, KEY_ID);
    try {
      Path path = dataDir.resolve(id + ".html");
      CharSink sink = asCharSink(path.toFile(), StandardCharsets.UTF_8);
      sink.write(message.message());
    } catch (IOException e) {
      throw new MessageStoreException("Failed to save message", e);
    }

    try {
      configFile.load();
      configFile.save();
    } catch (IOException | ConfigInvalidException e) {
      throw new MessageStoreException("Failed to save config", e);
    }
  }

  private void addAll(FileBasedConfig configFile, Config config) {
    for (String key : config.getNames(SECTION_MESSAGE, null)) {
      configFile.setString(
          SECTION_MESSAGE, null, key, config.getString(SECTION_MESSAGE, null, key));
    }
  }
}
