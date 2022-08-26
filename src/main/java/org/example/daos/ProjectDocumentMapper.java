package org.example.daos;

import org.bson.Document;
import org.example.models.ProjectDTO;

import java.time.LocalDateTime;

public class ProjectDocumentMapper {

    public static ProjectDTO mapToProjectDTO(Document document) {
        ProjectDTO projectDTO = new ProjectDTO();

        try {
            projectDTO.setId(document.getObjectId("_id"));
            projectDTO.setName(document.getString("name"));
            projectDTO.setLastUpdatedOriginHead((LocalDateTime) document.get("commit.committer_date"));

        } catch (Exception e) {
            System.out.println("Not able to map document to project: " + e.getMessage());
            System.exit(1);
        }

        return projectDTO;

    }

    public static Document mapToDocument(ProjectDTO projectDTO) {
        Document document = new Document();
        document.append("name", projectDTO.getName());
        document.append("last_updated_origin_head", projectDTO.getLastUpdatedOriginHead());
        return document;
    }
}
