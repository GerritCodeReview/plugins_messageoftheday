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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

@Singleton
public class FileBasedMessageStore extends AbstractMessageStore {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_EXPIRES_AT = "expiresAt";

  private final File cfgFile;
  private final Path dataDir;

  @Inject
  FileBasedMessageStore(@ConfigFile File cfgFile, @DataDir Path dataDir) {
    this.cfgFile = cfgFile;
    this.dataDir = dataDir;
  }

  @Override
  protected FileBasedConfig loadConfig() throws MessageStoreException {
    FileBasedConfig cfg = new FileBasedConfig(cfgFile, FS.DETECTED);
    try {
      cfg.load();
    } catch (ConfigInvalidException | IOException e) {
      throw new MessageStoreException("plugin cfg is invalid or could not be loaded", e);
    }
    return cfg;
  }

  @Override
  protected String loadMessage(String id) throws MessageStoreException {
    try {
      return new String(Files.readAllBytes(dataDir.resolve(id + ".html")), UTF_8);
    } catch (IOException e) {
      throw new MessageStoreException(e.getMessage(), e);
    }
  }

  @Override
  protected void setMessage(String message, Optional<ZonedDateTime> expiresAt)
      throws MessageStoreException {
    FileBasedConfig cfg = loadConfig();

    if (expiresAt.isEmpty()) {
      String expiredAtFromConfig = cfg.getString(SECTION_MESSAGE, null, KEY_EXPIRES_AT);
      if (expiredAtFromConfig == null) {
        throw new MessageStoreException(
            "expires_at is not provided for the current request and it is not "
                + "configured in the plugin cfg");
      }
    } else {
      cfg.setString(
          SECTION_MESSAGE,
          null,
          KEY_EXPIRES_AT,
          expiresAt.get().format(DateTimeFormatter.ofPattern("yyyyMMdd:HHmm")));
    }

    String id = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(id)) {
      logger.atInfo().log("'id' is not configured in the plugin cfg. Choosing a default id.");
      id = "default";
    }

    cfg.setString(SECTION_MESSAGE, null, KEY_ID, id);

    try {
      Path path = dataDir.resolve(id + ".html");
      CharSink sink = asCharSink(path.toFile(), StandardCharsets.UTF_8);
      sink.write(message);
    } catch (IOException e) {
      throw new MessageStoreException("Failed to save message", e);
    }

    try {
      cfg.save();
    } catch (IOException e) {
      throw new MessageStoreException("Failed to save plugin config", e);
    }
  }
}
