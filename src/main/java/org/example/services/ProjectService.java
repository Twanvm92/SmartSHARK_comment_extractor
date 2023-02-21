package org.example.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.daos.ConfigDAO;
import org.example.daos.ProjectDao;
import org.example.models.CommentDTO;

public class ProjectService {

  private final ProjectDao projectDao;
  private final HunkService hunkService;
  private final ConfigDAO configDAO;
  private final CommentService commentService;

  public ProjectService(
      ProjectDao projectDao,
      HunkService hunkService,
      ConfigDAO configDAO,
      CommentService commentService) {
    this.projectDao = projectDao;
    this.hunkService = hunkService;
    this.configDAO = configDAO;

    this.commentService = commentService;
  }

  public void addCommentsByProject(int limit, boolean outputOriginalHunks) {
    float secondInHour = 3600;
    if (outputOriginalHunks) {

      System.out.println("Retrieving and outputting initial hunks");
      Instant startQueryCount = Instant.now();

      projectDao.outputProjectsWithHunks();

      Instant endQueryCount = Instant.now();
      System.out.printf(
          "Hours passed since retrieving and outputting initial hunks: %.2f%n",
          Duration.between(startQueryCount, endQueryCount).toSeconds() / secondInHour);
    }

    System.out.println("Getting total hunks to be processed count..");
    long totalHunksCount = hunkService.getHunksCount();
    System.out.printf("%d hunks to be processed!%n", totalHunksCount);

    Instant startHunkCount = Instant.now();
    System.out.println("Hunk processing started!");

    ObjectId lastSeenObjectId;
    int amountHunksProcessed = 0;

    List<Document> hunks = hunkService.getHunks(limit);
    while (!hunks.isEmpty()) {
      List<CommentDTO> commentDTOS = new ArrayList<>();

      for (Document document : hunks) {

        List<String> addedLinesGroups =
            extractAddedLinesFromContent(document.get("hunk", Document.class).getString("content"));
        String projectName = document.getString("name");

        LocalDateTime committerDate =
            document
                .get("commit", Document.class)
                .getDate("committer_date")
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        ObjectId originalHunkId = document.get("hunk", Document.class).getObjectId("_id");
        int hunkNewStart = document.get("hunk", Document.class).getInteger("new_start");
        int hunkOldStart = document.get("hunk", Document.class).getInteger("old_start");

        ObjectId vcsId = document.get("vcs_system", Document.class).getObjectId("_id");
        String vcsUrl = document.get("vcs_system", Document.class).getString("url");
        ObjectId branchId = document.get("branch", Document.class).getObjectId("_id");
        String branchName = document.get("branch", Document.class).getString("name");

        ObjectId commitId = document.get("commit", Document.class).getObjectId("_id");
        String commitHash = document.get("commit", Document.class).getString("revision_hash");

        ObjectId fileActionId = document.get("file_action", Document.class).getObjectId("_id");

        ObjectId fileId = document.get("file", Document.class).getObjectId("_id");
        String filePath = document.get("file", Document.class).getString("path");

        for (String lineGroup : addedLinesGroups) {
          List<CommentDTO> comments = commentService.extractComments(lineGroup);

          if (!comments.isEmpty()) {
            comments =
                commentService.mapToCommentDTO(
                    projectName,
                    comments,
                    committerDate,
                    originalHunkId,
                    hunkNewStart,
                    hunkOldStart,
                    vcsId,
                    vcsUrl,
                    branchId,
                    branchName,
                    commitId,
                    commitHash,
                    fileActionId,
                    fileId,
                    filePath);
            commentDTOS.addAll(comments);
          }
        }
      }

      commentService.addComments(commentDTOS);

      lastSeenObjectId = hunks.get(hunks.size() - 1).getObjectId("_id");
      configDAO.addLastId(lastSeenObjectId);

      amountHunksProcessed += hunks.size();
      System.out.printf("%d/%d hunks processed %n", amountHunksProcessed, totalHunksCount);
      Instant endHunkCount = Instant.now();

      System.out.printf(
          "Hours passed since start of processing hunks: %.2f%n",
          Duration.between(startHunkCount, endHunkCount).toSeconds() / secondInHour);

      hunks = hunkService.getHunks(lastSeenObjectId, limit);
    }

    commentService.getAndAddDeduplicatedComments();
  }

  public void addCommentsByProject(ObjectId lastSeenId, int limit) {

    float secondInHour = 3600;
    System.out.println("Getting total hunks to be processed count..");
    long totalHunksCount = hunkService.getHunksCount(lastSeenId);
    System.out.printf("%d hunks to be processed!", totalHunksCount);

    Instant startHunkCount = Instant.now();
    System.out.println("Hunk processing started!");

    int amountHunksProcessed = 0;
    List<Document> hunks = hunkService.getHunks(lastSeenId, limit);

    while (!hunks.isEmpty()) {
      List<CommentDTO> commentDTOS = new ArrayList<>();

      for (Document document : hunks) {

        List<String> addedLinesGroups =
            extractAddedLinesFromContent(document.get("hunk", Document.class).getString("content"));
        String projectName = document.getString("name");

        LocalDateTime committerDate =
            document
                .get("commit", Document.class)
                .getDate("committer_date")
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        ObjectId originalHunkId = document.get("hunk", Document.class).getObjectId("_id");
        int hunkNewStart = document.get("hunk", Document.class).getInteger("new_start");
        int hunkOldStart = document.get("hunk", Document.class).getInteger("old_start");

        ObjectId vcsId = document.get("vcs_system", Document.class).getObjectId("_id");
        String vcsUrl = document.get("vcs_system", Document.class).getString("url");
        ObjectId branchId = document.get("branch", Document.class).getObjectId("_id");
        String branchName = document.get("branch", Document.class).getString("name");

        ObjectId commitId = document.get("commit", Document.class).getObjectId("_id");
        String commitHash = document.get("commit", Document.class).getString("revision_hash");

        ObjectId fileActionId = document.get("file_action", Document.class).getObjectId("_id");

        ObjectId fileId = document.get("file", Document.class).getObjectId("_id");
        String filePath = document.get("file", Document.class).getString("path");

        for (String lineGroup : addedLinesGroups) {
          List<CommentDTO> comments = commentService.extractComments(lineGroup);

          if (!comments.isEmpty()) {
            comments =
                commentService.mapToCommentDTO(
                    projectName,
                    comments,
                    committerDate,
                    originalHunkId,
                    hunkNewStart,
                    hunkOldStart,
                    vcsId,
                    vcsUrl,
                    branchId,
                    branchName,
                    commitId,
                    commitHash,
                    fileActionId,
                    fileId,
                    filePath);
            commentDTOS.addAll(comments);
          }
        }
      }

      commentService.addComments(commentDTOS);

      lastSeenId = hunks.get(hunks.size() - 1).getObjectId("_id");
      configDAO.addLastId(lastSeenId);

      amountHunksProcessed += hunks.size();
      System.out.printf("%d/%d hunks processed %n", amountHunksProcessed, totalHunksCount);
      Instant endHunkCount = Instant.now();

      System.out.printf(
          "Hours passed since start of processing hunks: %.2f%n",
          Duration.between(startHunkCount, endHunkCount).toSeconds() / secondInHour);

      hunks = hunkService.getHunks(lastSeenId, limit);
    }

    commentService.getAndAddDeduplicatedComments();
  }

  private List<String> extractAddedLinesFromContent(String content) {
    //    private final String ADDED_LINES_REGEX = "^\\+(?>.+(?>\\n\\+)*)+";
    String ADDED_LINES_REGEX = "^(?:\\+.+\\n?)+$";
    return Pattern.compile(ADDED_LINES_REGEX, Pattern.MULTILINE)
        .matcher(content)
        .results()
        .map(MatchResult::group)
        .collect(Collectors.toList())
        .stream()
        .map((str) -> str.replace("+", ""))
        .collect(Collectors.toList());
  }
}
