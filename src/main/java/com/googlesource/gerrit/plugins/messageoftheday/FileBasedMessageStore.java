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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

@Singleton
public class FileBasedMessageStore implements MessageStore {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_STARTS_AT = "startsAt";
  private static final String KEY_EXPIRES_AT = "expiresAt";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmm");

  private final File cfgFile;
  private final Path dataDir;

  @Inject
  FileBasedMessageStore(@ConfigFile File cfgFile, @DataDir Path dataDir) {
    this.cfgFile = cfgFile;
    this.dataDir = dataDir;
  }

  @Override
  public Optional<MessageOfTheDayInfo> getActiveMessage() throws MessageStoreException {
    FileBasedConfig cfg = loadConfig();

    String htmlFileId = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(htmlFileId)) {
      logger.atWarning().log("id not defined, no message will be shown");
      return Optional.empty();
    }

    MessageOfTheDayInfo motd = new MessageOfTheDayInfo();
    try {
      motd.expiresAt = DATE_FORMAT.parse(cfg.getString(SECTION_MESSAGE, null, KEY_EXPIRES_AT));
    } catch (ParseException | NullPointerException e) {
      logger.atWarning().log("expiresAt not defined, no message will be shown");
      return Optional.empty();
    }

    try {
      String startsAt = cfg.getString(SECTION_MESSAGE, null, KEY_STARTS_AT);
      motd.startsAt = Strings.isNullOrEmpty(startsAt) ? new Date() : DATE_FORMAT.parse(startsAt);
    } catch (ParseException e) {
      motd.startsAt = new Date();
    }

    if (motd.startsAt.compareTo(new Date()) > 0 || motd.expiresAt.compareTo(new Date()) < 0) {
      logger.atFine().log("Current date/time is outside of the startsAt..expiresAt interval");
      return Optional.empty();
    }

    try {
      motd.html = new String(Files.readAllBytes(dataDir.resolve(htmlFileId + ".html")), UTF_8);
    } catch (IOException e1) {
      logger.atWarning().log(
          "No HTML-file was found for message %s, no message will be shown", htmlFileId);
      return Optional.empty();
    }

    motd.id = Integer.toString(motd.html.hashCode());
    return Optional.of(motd);
  }

  @Override
  public void setMessage(String message) throws MessageStoreException {
    setMessage(message, Optional.empty());
  }

  @Override
  public void setMessage(String message, ZonedDateTime expiresAt) throws MessageStoreException {
    setMessage(message, Optional.of(expiresAt));
  }

  private void setMessage(String message, Optional<ZonedDateTime> expiresAt)
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

  private FileBasedConfig loadConfig() throws MessageStoreException {
    FileBasedConfig cfg = new FileBasedConfig(cfgFile, FS.DETECTED);
    try {
      cfg.load();
    } catch (ConfigInvalidException | IOException e) {
      throw new MessageStoreException("plugin cfg is invalid or could not be loaded", e);
    }
    return cfg;
  }
}
