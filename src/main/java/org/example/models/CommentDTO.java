package org.example.models;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@Data
@EqualsAndHashCode
public class CommentDTO {
  @EqualsAndHashCode.Exclude @BsonId private ObjectId id;

  @EqualsAndHashCode.Exclude
  @BsonProperty(value = "project_name")
  private String projectName;

  @BsonProperty(value = "hunk_id")
  private ObjectId hunkId;

  @BsonProperty(value = "hunk_new_start")
  @EqualsAndHashCode.Exclude
  private int hunkNewStart;

  @BsonProperty(value = "hunk_old_start")
  @EqualsAndHashCode.Exclude
  private int hunkOldStart;

  @BsonProperty(value = "vcs_id")
  @EqualsAndHashCode.Exclude
  private ObjectId vcsId;

  @BsonProperty(value = "vcs_url")
  @EqualsAndHashCode.Exclude
  private String vcsUrl;

  @BsonProperty(value = "branch_id")
  @EqualsAndHashCode.Exclude
  private ObjectId branchId;

  @BsonProperty(value = "branch_name")
  @EqualsAndHashCode.Exclude
  private String branchName;

  @NonNull private String content;

  @EqualsAndHashCode.Exclude
  @BsonProperty(value = "committer_date")
  private LocalDateTime committerDate;

  @BsonProperty(value = "commit_id")
  @EqualsAndHashCode.Exclude
  private ObjectId commitId;

  @BsonProperty(value = "commit_hash")
  @EqualsAndHashCode.Exclude
  private String commitHash;

  @BsonProperty(value = "file_action_id")
  @EqualsAndHashCode.Exclude
  private ObjectId fileActionId;

  @BsonProperty(value = "file_id")
  @EqualsAndHashCode.Exclude
  private ObjectId fileId;

  @BsonProperty(value = "file_path")
  @EqualsAndHashCode.Exclude
  private String filePath;

  @EqualsAndHashCode.Exclude @NonNull private boolean filtered;
  @EqualsAndHashCode.Exclude @NonNull private CommentType type;
}
