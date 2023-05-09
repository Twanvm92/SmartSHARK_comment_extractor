package org.example.services;

import java.text.ParseException;
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

  /**
   * Retrieves and processes hunks of code, extracting comments and storing them in the database.
   *
   * <p>If outputOriginalHunks is true, it also outputs the initial hunks.
   *
   * @param limit the maximum number of hunks to process at once
   * @param outputOriginalHunks whether or not to extract the initial hunks from the SmartSHARK
   *     database and persist them in the intermediate database for further processing. If set to
   *     False, will assume initial (time-consuming) collection of hunks from SmartSHARK database
   *     has already happened. Continue to retrieve comments from the extracted hunks.
   */
  public void addCommentsByProject(int limit, boolean outputOriginalHunks) throws ParseException {
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
  }

  //  TODO can be used to start extracting comments from a specific hunk_id
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
  }

  /**
   * Extracts added lines from the provided content string. Added lines are lines that start with a
   * "+" symbol.
   *
   * @param content the content to extract added lines from
   * @return a list of strings containing the added lines from the content
   */
  public List<String> extractAddedLinesFromContent(String content) {
    String ADDED_LINES_REGEX = "^(?:\\+.+\\r?\\n?)+$";
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
