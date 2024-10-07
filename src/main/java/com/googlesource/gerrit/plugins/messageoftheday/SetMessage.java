// Copyright (C) 2024 The Android Open Source Project
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.CharSink;
import com.google.common.io.Files;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.ConfigUtil;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

public class SetMessage implements RestModifyView<ConfigResource, MessageInput> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_EXPIRES_AT = "expiresAt";
  private final String pluginName;
  private final File cfgFile;
  private final Path dataDirPath;
  private final PermissionBackend permissionBackend;
  private final UpdateBannerPermission permission;

  private volatile FileBasedConfig cfg;

  @Inject
  public SetMessage(
      @PluginName String pluginName,
      @ConfigFile File cfgFile,
      @DataDir Path dataDirPath,
      PermissionBackend permissionBackend,
      UpdateBannerPermission permission) {
    this.pluginName = pluginName;
    this.dataDirPath = dataDirPath;
    this.cfgFile = cfgFile;
    this.permission = permission;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<?> apply(ConfigResource resource, MessageInput input)
      throws AuthException, BadRequestException, ResourceConflictException,
          PermissionBackendException, ConfigInvalidException, UnprocessableEntityException {
    permissionBackend.currentUser().check(permission);

    if (input.message == null) {
      throw new BadRequestException("message is required");
    }

    cfg = new FileBasedConfig(cfgFile, FS.DETECTED);
    try {
      cfg.load();
    } catch (ConfigInvalidException | IOException e) {
      return null;
    }

    String htmlFileId = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(htmlFileId)) {
      throw new ConfigInvalidException("No id configured");
    }

    Path path = dataDirPath.resolve(htmlFileId + ".html");
    CharSink sink = Files.asCharSink(path.toFile(), StandardCharsets.UTF_8);

    Instant expiresAt = null;
    if (input.expiresAfter != null) {
      long ms =
          MILLISECONDS.convert(ConfigUtil.getTimeUnit(input.expiresAfter, 0, SECONDS), SECONDS);
      expiresAt = Instant.ofEpochMilli(TimeUtil.nowMs() + ms);
      DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern("yyyyMMdd:HHmm").withZone(ZoneId.systemDefault());
      logger.atSevere().log("date %s", expiresAt.toString());
      cfg.setString(SECTION_MESSAGE, null, KEY_EXPIRES_AT, formatter.format(expiresAt));
    }

    try {
      sink.write(input.message);
    } catch (IOException e) {
      throw new UnprocessableEntityException("Failed to save message", e);
    }

    if (expiresAt != null) {
      try {
        cfg.save();
      } catch (IOException e) {
        throw new UnprocessableEntityException("Failed to save expire_after", e);
      }
    }

    return Response.ok();
  }
}