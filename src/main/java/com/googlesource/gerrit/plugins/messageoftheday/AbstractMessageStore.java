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

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;

public abstract class AbstractMessageStore implements MessageStore {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_STARTS_AT = "startsAt";
  private static final String KEY_EXPIRES_AT = "expiresAt";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmm");

  @Override
  public final Optional<MessageOfTheDayInfo> getActiveMessage() throws MessageStoreException {
    Config cfg = loadConfig();
    String id = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(id)) {
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

    motd.html = loadMessage(id);
    return Optional.of(motd);
  }

  protected abstract Config loadConfig() throws MessageStoreException;

  protected abstract String loadMessage(String id) throws MessageStoreException;

  @Override
  public final void setMessage(String message) throws MessageStoreException {
    setMessage(message, Optional.empty());
  }

  @Override
  public final void setMessage(String message, ZonedDateTime expiresAt)
      throws MessageStoreException {
    setMessage(message, Optional.of(expiresAt));
  }

  protected abstract void setMessage(String message, Optional<ZonedDateTime> expiresAt)
      throws MessageStoreException;
}
