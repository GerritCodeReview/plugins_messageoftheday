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

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.PersonIdent;

public class SetMessage implements RestModifyView<ConfigResource, MessageInput> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";
  private static final String KEY_EXPIRES_AT = "expiresAt";

  private static final String INPUT_DATE_FORMAT_PATTERN = "MM/dd/yyyy, hh:mm a [O][z]";
  private static final DateTimeFormatter INPUT_DATE_FORMAT =
      DateTimeFormatter.ofPattern(INPUT_DATE_FORMAT_PATTERN, Locale.ENGLISH);

  private final MessageStore messageStore;
  private final ZoneId serverZoneId;
  private final PermissionBackend permissionBackend;
  private final UpdateBannerPermission permission;

  @Inject
  public SetMessage(
      MessageStore messageStore,
      @GerritPersonIdent Provider<PersonIdent> serverIdent,
      PermissionBackend permissionBackend,
      UpdateBannerPermission permission) {
    this.messageStore = messageStore;
    this.serverZoneId = serverIdent.get().getZoneId();
    this.permission = permission;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<?> apply(ConfigResource resource, MessageInput input)
      throws AuthException,
          BadRequestException,
          ResourceConflictException,
          PermissionBackendException,
          ConfigInvalidException,
          UnprocessableEntityException {
    permissionBackend.currentUser().check(permission);

    if (input.message == null) {
      throw new BadRequestException("message is required");
    }

    ConfiguredMessage configuredMessage;
    try {
      configuredMessage = messageStore.getConfiguredMessage();
    } catch (MessageStoreException e) {
      throw new UnprocessableEntityException(e.getMessage(), e);
    }

    Config cfg = configuredMessage.config();

    String id = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    if (Strings.isNullOrEmpty(id)) {
      logger.atInfo().log("'id' is not configured in the plugin cfg. Choosing a default id.");
      cfg.setString(SECTION_MESSAGE, null, KEY_ID, "default");
    }

    if (input.expiresAt != null) {
      ZonedDateTime time;
      try {
        time =
            ZonedDateTime.parse(input.expiresAt, INPUT_DATE_FORMAT)
                .withZoneSameInstant(serverZoneId);
      } catch (IllegalArgumentException e) {
        throw new BadRequestException(
            "Invalid value for expires_at. It must be provided in '"
                + INPUT_DATE_FORMAT_PATTERN
                + "' format");
      }
      cfg.setString(
          SECTION_MESSAGE,
          null,
          KEY_EXPIRES_AT,
          time.format(DateTimeFormatter.ofPattern("yyyyMMdd:HHmm")));
    } else {
      String expiredAt = cfg.getString(SECTION_MESSAGE, null, KEY_EXPIRES_AT);
      if (expiredAt == null) {
        throw new UnprocessableEntityException(
            "expires_at is not provided for the current request and it is not "
                + "configured in the plugin cfg");
      }
    }

    try {
      messageStore.saveConfiguredMessage(ConfiguredMessage.create(cfg, input.message));
    } catch (MessageStoreException e) {
      throw new UnprocessableEntityException(e.getMessage(), e);
    }

    return Response.ok();
  }
}
