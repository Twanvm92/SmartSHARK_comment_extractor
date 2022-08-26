package org.example.services;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.daos.*;
import org.example.models.CommentDTO;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProjectService {
    private ProjectDao projectDao;
    private HunkService hunkService;
    private ConfigDAO configDAO;
    private CommentService commentService;
    private final String ADDED_LINES_REGEX = "\\+.+";

    public ProjectService(ProjectDao projectDao, HunkService hunkService, ConfigDAO configDAO, CommentService commentService) {
        this.projectDao = projectDao;
        this.hunkService = hunkService;
        this.configDAO = configDAO;

        this.commentService = commentService;

    }

    public void addCommentsByProject(int limit) {
//        TODO do parameter overloading => one with limit and lastseenID where we do not get retrieve
//         and output initial hunks and one with just limit where we do retrieve and output initial hunks
//          and start processing hunks from start

        System.out.println("Retrieving and outputting initial hunks");
        Instant startQueryCount = Instant.now();

        projectDao.outputProjectsWithHunks();

        float secondInHour = 3600;
        Instant endQueryCount = Instant.now();
        System.out.printf("Hours passed since retrieving and outputting initial hunks: %.2f%n",
                Duration.between(startQueryCount, endQueryCount).toSeconds()/secondInHour);

        long totalHunksCount = hunkService.getHunksCount();
        System.out.printf("%d hunks to be processed!", totalHunksCount);

        Instant startHunkCount = Instant.now();
        System.out.println("Hunk processing started!");

        ObjectId lastSeenObjectId;
        int amountHunksProcessed = 0;

        List<Document> hunks = hunkService.getHunks(limit);
        while (!hunks.isEmpty()) {
            List<CommentDTO> commentDTOS = new ArrayList<>();

            for (Document document: hunks) {

                List<String> addedLinesGroups = extractAddedLinesFromContent(
                        document
                                .get("hunk", Document.class)
                                .getString("content"));
                String projectName = document.getString("name");

                LocalDateTime committerDate = document
                        .get("commit", Document.class)
                        .getDate("committer_date")
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                for (String lineGroup: addedLinesGroups) {
                    List<CommentDTO> comments = commentService.extractComments(lineGroup);

                    if (!comments.isEmpty()) {
                        comments = commentService.mapToCommentDTO(projectName, comments, committerDate);
                        commentDTOS.addAll(comments);
                    }
                }
            }

            commentService.addComments(commentDTOS);

            lastSeenObjectId = hunks.get(hunks.size() - 1).getObjectId("_id");
//            TODO persist last seen objectId
//            configDAO.addLastSkipAmount(skip);

            amountHunksProcessed += hunks.size();
            System.out.printf("%d/%d hunks processed %n", amountHunksProcessed, totalHunksCount);
            Instant endHunkCount = Instant.now();

            System.out.printf("Hours passed since start of processing hunks: %.2f%n",
                    Duration.between(startHunkCount, endHunkCount).toSeconds()/secondInHour);

            hunks = hunkService.getHunks(lastSeenObjectId, limit);
        }

//        TODO uncomment!
//         only uncomment this when all comments are already in comment collection.
//        commentService.getAndAddDeduplicatedComments();
    }

    public void addCommentsByProject(ObjectId lastSeenId, int limit) {

//        Integer queryCount = projectDao.getProjectWithHunksCountAndAddHunks()
//                .orElse(new Document("count", 0))
//                .getInteger("count");

        float secondInHour = 3600;
        long totalHunksCount = hunkService.getHunksCount(lastSeenId);
        System.out.printf("%d hunks to be processed!", totalHunksCount);

        Instant startHunkCount = Instant.now();
        System.out.println("Hunk processing started!");
//        List<Document> projectsWithHunks = projectDao.getProjectWithHunks(skip, limit);


        int amountHunksProcessed = 0;
        List<Document> hunks = hunkService.getHunks(lastSeenId, limit);

        while (!hunks.isEmpty()) {
            List<CommentDTO> commentDTOS = new ArrayList<>();

            for (Document document: hunks) {

                List<String> addedLinesGroups = extractAddedLinesFromContent(
                        document
                                .get("hunk", Document.class)
                                .getString("content"));
                String projectName = document.getString("name");

                LocalDateTime committerDate = document
                        .get("commit", Document.class)
                        .getDate("committer_date")
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                for (String lineGroup: addedLinesGroups) {
                    List<CommentDTO> comments = commentService.extractComments(lineGroup);

                    if (!comments.isEmpty()) {
                        comments = commentService.mapToCommentDTO(projectName, comments, committerDate);
                        commentDTOS.addAll(comments);
                    }
                }
            }

//            TODO separate project documents not needed just query comments collection for statistics?
//            BulkWriteResult bulkWriteResultProj = projectDao.addProjects(projectsWithHunks);
//            TODO do not add hunks anymore to intermed database?
//            BulkWriteResult bulkWriteResultHunk = hunkDAO.addHunks(projectsWithHunks);

            commentService.addComments(commentDTOS);

            lastSeenId = hunks.get(hunks.size() - 1).getObjectId("_id");
//            TODO persist last seen objectId
//            configDAO.addLastSkipAmount(skip);

            amountHunksProcessed += hunks.size();
            System.out.printf("%d/%d hunks processed %n", amountHunksProcessed, totalHunksCount);
            Instant endHunkCount = Instant.now();

            System.out.printf("Hours passed since start of processing hunks: %.2f%n",
                    Duration.between(startHunkCount, endHunkCount).toSeconds()/secondInHour);

            hunks = hunkService.getHunks(lastSeenId, limit);
        }

//        TODO need to uncomment!
//        commentService.getAndAddDeduplicatedComments();
    }

    private List<String> extractAddedLinesFromContent(String content) {
        return Pattern
                .compile(ADDED_LINES_REGEX, Pattern.MULTILINE)
                .matcher(content)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.toList());

    }
}
