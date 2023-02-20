package org.example.models;

import java.time.LocalDateTime;
import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@Data
public class ProjectDTO {
  @BsonId private ObjectId id;
  private String name;

  @BsonProperty(value = "last_updated_origin_head")
  private LocalDateTime lastUpdatedOriginHead;
}
