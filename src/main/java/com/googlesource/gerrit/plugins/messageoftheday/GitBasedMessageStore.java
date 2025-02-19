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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

@Singleton
public class GitBasedMessageStore implements MessageStore {
  public static String MASTER = Constants.R_HEADS + Constants.MASTER;

  private static final String SECTION_MESSAGE = "message";
  private static final String KEY_ID = "id";

  private final GitBasedConfigReader configReader;
  private final GitReferenceUpdated gitRefUpdated;
  private final NameKey configRepoName;
  private final Repository configRepo;
  private final Provider<CurrentUser> currentUser;
  private final PersonIdent gerritIdent;

  @Inject
  GitBasedMessageStore(
      GitBasedConfigReader configReader,
      GitReferenceUpdated gitRefUpdated,
      @GitConfigRepo Project.NameKey configRepoName,
      @GitConfigRepo Repository configRepo,
      @GerritPersonIdent PersonIdent gerritIdent,
      Provider<CurrentUser> currentUser) {
    this.configReader = configReader;
    this.gitRefUpdated = gitRefUpdated;
    this.configRepoName = configRepoName;
    this.configRepo = configRepo;
    this.gerritIdent = gerritIdent;
    this.currentUser = currentUser;
  }

  @Override
  public ConfiguredMessage getConfiguredMessage() throws MessageStoreException {
    CachedConfig cached = configReader.getConfig();
    return ConfiguredMessage.create(cached.config(), cached.message());
  }

  @Override
  public void saveConfiguredMessage(ConfiguredMessage message) throws MessageStoreException {
    CachedConfig cached = configReader.getConfig();

    RefUpdate ru;
    try {
      ru = prepareRefUpdate(message.message(), configRepo, cached);
      RefUpdate.Result result = ru.update();
      switch (result) {
        case NEW:
        case FAST_FORWARD:
          if (currentUser.get().isIdentifiedUser()) {
            gitRefUpdated.fire(configRepoName, ru, currentUser.get().asIdentifiedUser().state());
          }
          return;
        case LOCK_FAILURE:
        case FORCED:
        case IO_FAILURE:
        case NOT_ATTEMPTED:
        case NO_CHANGE:
        case REJECTED:
        case REJECTED_CURRENT_BRANCH:
        case RENAMED:
        case REJECTED_MISSING_OBJECT:
        case REJECTED_OTHER_REASON:
        default:
          throw new MessageStoreException("Couldn't save message");
      }
    } catch (IOException e) {
      throw new MessageStoreException(e.getMessage(), e);
    }
  }

  private RefUpdate prepareRefUpdate(String message, Repository repo, CachedConfig cachedConfig)
      throws IOException {
    Config cfg = cachedConfig.config();
    ObjectInserter inserter = repo.newObjectInserter();

    DirCache newTree = DirCache.newInCore();
    DirCacheEditor editor = newTree.editor();
    ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, cfg.toText().getBytes(UTF_8));

    editor.add(
        new DirCacheEditor.PathEdit("messageoftheday.config") {
          @Override
          public void apply(DirCacheEntry ent) {
            ent.setFileMode(FileMode.REGULAR_FILE);
            ent.setObjectId(blobId);
          }
        });

    ObjectId messageBlobId = inserter.insert(Constants.OBJ_BLOB, message.getBytes(UTF_8));
    String id = cfg.getString(SECTION_MESSAGE, null, KEY_ID);
    editor.add(
        new DirCacheEditor.PathEdit(id + ".html") {
          @Override
          public void apply(DirCacheEntry ent) {
            ent.setFileMode(FileMode.REGULAR_FILE);
            ent.setObjectId(messageBlobId);
          }
        });
    editor.finish();

    ObjectId treeId = newTree.writeTree(inserter);
    CommitBuilder cb = new CommitBuilder();
    cb.setParentId(cachedConfig.commitId());
    cb.setTreeId(treeId);
    Instant now = TimeUtil.now();
    PersonIdent author =
        currentUser.get().asIdentifiedUser().newCommitterIdent(now, gerritIdent.getZoneId());
    cb.setCommitter(gerritIdent);
    cb.setAuthor(author);
    cb.setMessage("Update from REST API");
    ObjectId newCommitId = inserter.insert(cb);
    inserter.flush();

    RefUpdate ru = repo.updateRef(MASTER);
    ru.setExpectedOldObjectId(cachedConfig.commitId());
    ru.setNewObjectId(newCommitId);
    ru.setRefLogIdent(author);
    ru.setRefLogMessage("Update from REST API", false);
    return ru;
  }
}
