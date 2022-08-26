package org.example.models;

import lombok.Data;

import java.util.LinkedList;

@Data
public class Project {
    private String name;
    private LinkedList<CommentDTO> comments;
}
