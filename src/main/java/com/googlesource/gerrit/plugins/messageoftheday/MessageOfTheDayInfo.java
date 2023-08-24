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

import java.sql.Timestamp;
import java.util.Date;

/** REST API representation of a "message of the day". */
public class MessageOfTheDayInfo {
  /** The ID of the message. */
  public String id;
  /** The time from which on the message will be displayed. */
  public Date startsAt;
  /** The time from which on the message will not be displayed anymore. */
  public Date expiresAt;
  /** The date and time the message will be displayed again after being dismissed by the user. */
  public Timestamp redisplay;
  /** The message in HTML-format. */
  public String html;
}
