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

import com.google.common.base.Strings;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetMessage implements RestReadView<ConfigResource> {
  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_STARTS_AT = "startsAt";
  private static final String KEY_EXPIRES_AT = "expiresAt";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmm");

  private static final Logger log = LoggerFactory.getLogger(GetMessage.class);

  private final MessageStore messageStore;

  @Inject
  public GetMessage(MessageStore messageStore) {
    this.messageStore = messageStore;
  }

  @Override
  public Response<MessageOfTheDayInfo> apply(ConfigResource rsrc) {
    MessageOfTheDayInfo motd = new MessageOfTheDayInfo();
    ConfiguredMessage configuredMessage;
    try {
      configuredMessage = messageStore.getConfiguredMessage();
    } catch (MessageStoreException e) {
      log.warn(e.getMessage());
      return Response.none();
    }
    Config cfg = configuredMessage.config();
    String message = configuredMessage.message();
    if (cfg == null || message == null) {
      return Response.none();
    }

    try {
      motd.expiresAt = DATE_FORMAT.parse(cfg.getString(SECTION_MESSAGE, null, KEY_EXPIRES_AT));
    } catch (ParseException | NullPointerException e) {
      log.warn("expiresAt not defined, no message will be shown");
      return Response.none();
    }

    try {
      String startsAt = cfg.getString(SECTION_MESSAGE, null, KEY_STARTS_AT);
      motd.startsAt = Strings.isNullOrEmpty(startsAt) ? new Date() : DATE_FORMAT.parse(startsAt);
    } catch (ParseException e) {
      motd.startsAt = new Date();
    }

    if (motd.startsAt.compareTo(new Date()) > 0 || motd.expiresAt.compareTo(new Date()) < 0) {
      log.debug("Current date/time is outside of the startsAt..expiresAt interval");
      return Response.none();
    }

    motd.html = message;
    motd.id = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    return Response.ok(motd);
  }
}
