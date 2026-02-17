// Copyright (C) 2026 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileBasedMessageStoreTest {
  private static final String SECTION_MESSAGE = "message";
  private static final String MESSAGE_ID = "test-message";
  private static final String HTML_CONTENT = "<p>Hello World</p>";
  private static final String EXPIRES_AT = "20260323:1400";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private File cfgFile;
  private Path dataDir;
  private FileBasedMessageStore store;

  @Before
  public void setUp() throws Exception {
    cfgFile = tempFolder.newFile("messageoftheday.config");
    dataDir = tempFolder.newFolder("data").toPath();
    store = new FileBasedMessageStore(cfgFile, dataDir);
  }

  @Test
  public void writesHtmlFileWithCorrectContent() throws Exception {
    Config config = new Config();
    config.setString(SECTION_MESSAGE, null, "id", MESSAGE_ID);

    store.saveConfiguredMessage(new ConfiguredMessage(config, HTML_CONTENT));

    Path htmlFile = dataDir.resolve(MESSAGE_ID + ".html");
    assertThat(Files.exists(htmlFile)).isTrue();
    assertThat(Files.readString(htmlFile, UTF_8)).isEqualTo(HTML_CONTENT);
  }

  @Test
  public void writesConfigFileWithCorrectValues() throws Exception {
    Config config = new Config();
    config.setString(SECTION_MESSAGE, null, "id", MESSAGE_ID);
    config.setString(SECTION_MESSAGE, null, "expiresAt", EXPIRES_AT);

    store.saveConfiguredMessage(new ConfiguredMessage(config, HTML_CONTENT));

    FileBasedConfig savedConfig = new FileBasedConfig(cfgFile, FS.DETECTED);
    savedConfig.load();
    assertThat(savedConfig.getString(SECTION_MESSAGE, null, "id")).isEqualTo(MESSAGE_ID);
    assertThat(savedConfig.getString(SECTION_MESSAGE, null, "expiresAt")).isEqualTo(EXPIRES_AT);
  }
}
