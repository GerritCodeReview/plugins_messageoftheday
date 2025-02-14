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
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.PersonIdent;

public class SetMessage implements RestModifyView<ConfigResource, MessageInput> {
  private final ZoneId serverZoneId;
  private final PermissionBackend permissionBackend;
  private final UpdateBannerPermission permission;

  private final MessageStore messageStore;

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

    ZonedDateTime time = getExpiresAt(input);
    try {
      if (time == null) {
        messageStore.setMessage(input.message);
      } else {
        messageStore.setMessage(input.message, time);
      }
    } catch (MessageStoreException e) {
      throw new UnprocessableEntityException(e.getMessage(), e);
    }

    return Response.ok();
  }

  private ZonedDateTime getExpiresAt(MessageInput input) throws BadRequestException {
    if (input.expiresAt != null) {
      try {
        return ZonedDateTime.parse(
                input.expiresAt, DateTimeFormatter.ofPattern("MM/dd/yyyy, hh:mm a z"))
            .withZoneSameInstant(serverZoneId);
      } catch (IllegalArgumentException e) {
        throw new BadRequestException(
            "Invalid value for expires_at. It must be provided in 'MM/dd/yyyy, hh:mm a z' format");
      }
    }
    return null;
  }
}
