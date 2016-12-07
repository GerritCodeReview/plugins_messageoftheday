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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.systemstatus.MessageOfTheDay;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Singleton
class MessageOfTheDayImpl extends MessageOfTheDay {
  private static final Logger log = LoggerFactory.getLogger(MessageOfTheDayImpl.class);

  private static final DateFormat YYYYMMdd = new SimpleDateFormat("YYYYMMdd");

  private final File data;
  private final String id;
  private final String expiresAt;
  private final String msg;

  @Inject
  public MessageOfTheDayImpl(
      PluginConfigFactory configFactory,
      @PluginName String myName,
      @PluginData File data) {
    this.data = data;
    Config cfg = configFactory.getGlobalPluginConfig(myName);
    id = cfg.getString("message", null, "id");
    expiresAt = cfg.getString("message", null, "expiresAt");
    msg = message();
  }

  @Override
  public String getHtmlMessage() {
    if (Strings.isNullOrEmpty(id)) {
      return null;
    }

    if (Strings.isNullOrEmpty(expiresAt)) {
      log.warn("expiresAt not defined, no message will be shown");
      return null;
    }

    if (today().compareTo(expiresAt) > 0) {
      return null;
    }

    return msg;
  }

  @Override
  public String getMessageId() {
    return id;
  }

  private String message() {
    if (Strings.isNullOrEmpty(id)) {
      return null;
    }

    Path p = new File(data, id + ".html").toPath();
    try {
      return new String(Files.readAllBytes(p), UTF_8);
    } catch (IOException e) {
      log.warn("Couldn't read content of the mesage with id = " + id, e);
      return null;
    }
  }

  private static String today() {
    return YYYYMMdd.format(new Date());
  }
}
