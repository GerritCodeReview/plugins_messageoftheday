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
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetMessage implements RestReadView<ConfigResource> {
  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_STARTS_AT = "startsAt";
  private static final String KEY_EXPIRES_AT = "expiresAt";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmm");

  private static final Logger log = LoggerFactory.getLogger(GetMessage.class);

  private final File cfgFile;
  private final Path dataDirPath;

  private volatile FileBasedConfig cfg;

  @Inject
  public GetMessage(
      @PluginName String pluginName, @PluginData Path dataDirPath, SitePaths sitePaths) {
    this.dataDirPath = dataDirPath;
    this.cfgFile = sitePaths.etc_dir.resolve(pluginName + ".config").toFile();
  }

  @Override
  public MessageOfTheDayInfo apply(ConfigResource rsrc) {
    MessageOfTheDayInfo motd = new MessageOfTheDayInfo();
    cfg = new FileBasedConfig(cfgFile, FS.DETECTED);
    try {
      cfg.load();
    } catch (ConfigInvalidException | IOException e) {
      return null;
    }

    motd.id = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(motd.id)) {
      log.warn("id not defined, no message will be shown");
      return null;
    }

    try {
      motd.expiresAt = DATE_FORMAT.parse(cfg.getString(SECTION_MESSAGE, null, KEY_EXPIRES_AT));
    } catch (ParseException | NullPointerException e) {
      log.warn("expiresAt not defined, no message will be shown");
      return null;
    }

    try {
      motd.html = readHtmlFile(dataDirPath.resolve(motd.id + ".html"));
    } catch (IOException e1) {
      log.warn(
          String.format(
              "No HTML-file was found for message %s, no message will be shown", motd.id));
      return null;
    }

    try {
      String startsAt = cfg.getString(SECTION_MESSAGE, null, KEY_STARTS_AT);
      motd.startsAt = Strings.isNullOrEmpty(startsAt) ? new Date() : DATE_FORMAT.parse(startsAt);
    } catch (ParseException e) {
      motd.startsAt = new Date();
    }

    if (motd.startsAt.compareTo(new Date()) > 0 || motd.expiresAt.compareTo(new Date()) < 0) {
      return null;
    }

    motd.redisplay = getRedisplay();

    return motd;
  }

  private String readHtmlFile(Path filePath) throws IOException {
    StringBuilder htmlBuilder = new StringBuilder();
    BufferedReader br = Files.newBufferedReader(filePath);
    String currentLine = br.readLine();
    while (!Strings.isNullOrEmpty(currentLine)) {
      htmlBuilder.append(currentLine);
      htmlBuilder.append("\n");
      currentLine = br.readLine();
    }
    return htmlBuilder.toString();
  }

  private Date getRedisplay() {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    cal.add(Calendar.DAY_OF_MONTH, 1);
    return cal.getTime();
  }
}
