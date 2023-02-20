package org.example.models;

import java.util.LinkedList;
import lombok.Data;

@Data
public class Project {
  private String name;
  private LinkedList<CommentDTO> comments;
}
