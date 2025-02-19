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

import com.google.auto.value.AutoValue;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;

@AutoValue
public abstract class CachedConfig {
  public static CachedConfig empty() {
    return new AutoValue_CachedConfig(null, null, null);
  }

  public static CachedConfig create(Config config, String message, ObjectId commitId) {
    return new AutoValue_CachedConfig(config, message, commitId);
  }

  public abstract Config config();

  public abstract String message();

  public abstract ObjectId commitId();
}
