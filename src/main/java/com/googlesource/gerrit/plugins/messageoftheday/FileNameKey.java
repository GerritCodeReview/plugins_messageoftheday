package com.googlesource.gerrit.plugins.messageoftheday;

import com.google.auto.value.AutoValue;
import com.google.gerrit.entities.Project;
import org.eclipse.jgit.lib.ObjectId;

@AutoValue
public abstract class FileNameKey {
  public static FileNameKey create(Project.NameKey project, ObjectId rev, String file) {
    return new AutoValue_FileNameKey(project, rev, file);
  }

  public abstract Project.NameKey project();

  public abstract ObjectId rev();

  public abstract String file();
}
