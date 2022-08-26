package org.example.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode
public class CommentDTO {
    @EqualsAndHashCode.Exclude
    @BsonId
    private ObjectId id;
    @EqualsAndHashCode.Exclude
    @BsonProperty(value = "project_name")
    private String projectName;
    @NonNull
    private String content;
    @EqualsAndHashCode.Exclude
    @BsonProperty(value = "committer_date")
    private LocalDateTime committerDate;
    @EqualsAndHashCode.Exclude
    @NonNull
    private boolean filtered;
    @EqualsAndHashCode.Exclude
    @NonNull
    private CommentType type;
}
