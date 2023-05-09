package org.example.services;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.CommentsCollection;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.example.daos.CommentDao;
import org.example.models.CommentDTO;
import org.example.models.CommentType;

/** A service for managing comments in source code files. */
public class CommentService {

  private final CommentDao commentDao;
  private final JavaParser parser;

  /**
   * Constructs a new CommentService object.
   *
   * @param commentDao the data access object used to store and retrieve comments
   */
  public CommentService(CommentDao commentDao) {
    this.commentDao = commentDao;
    parser = new JavaParser();
    parser.getParserConfiguration().setPreprocessUnicodeEscapes(true);
  }

  /**
   * Maps a list of CommentDTO objects to a new list of CommentDTO objects, setting additional
   * properties for each CommentDTO.
   *
   * @param projectName the name of the project
   * @param comments the list of CommentDTO objects to map
   * @param committerDate the date the comment was committed
   * @param originalHunkId the ID of the original hunk
   * @param hunkNewStart the starting line number of the new hunk
   * @param hunkOldStart the starting line number of the old hunk
   * @param vcsId the ID of the version control system
   * @param vcsUrl the URL of the version control system
   * @param branchId the ID of the branch
   * @param branchName the name of the branch
   * @param commitId the ID of the commit
   * @param commitHash the hash of the commit
   * @param fileActionId the ID of the file action
   * @param fileId the ID of the file
   * @param filePath the path of the file
   * @return a new list of CommentDTO objects
   */
  public List<CommentDTO> mapToCommentDTO(
      String projectName,
      List<CommentDTO> comments,
      LocalDateTime committerDate,
      ObjectId originalHunkId,
      int hunkNewStart,
      int hunkOldStart,
      ObjectId vcsId,
      String vcsUrl,
      ObjectId branchId,
      String branchName,
      ObjectId commitId,
      String commitHash,
      ObjectId fileActionId,
      ObjectId fileId,
      String filePath) {
    return comments.stream()
        .peek(
            (commentDTO) -> {
              commentDTO.setProjectName(projectName);
              commentDTO.setCommitterDate(committerDate);
              commentDTO.setHunkId(originalHunkId);
              commentDTO.setHunkNewStart(hunkNewStart);
              commentDTO.setHunkOldStart(hunkOldStart);
              commentDTO.setVcsId(vcsId);
              commentDTO.setVcsUrl(vcsUrl);
              commentDTO.setBranchId(branchId);
              commentDTO.setBranchName(branchName);
              commentDTO.setCommitId(commitId);
              commentDTO.setCommitHash(commitHash);
              commentDTO.setFileActionId(fileActionId);
              commentDTO.setFileId(fileId);
              commentDTO.setFilePath(filePath);
            })
        .collect(Collectors.toList());
  }

  /**
   * Adds a list of CommentDTO objects to the data store.
   *
   * @param commentDTOs the list of CommentDTO objects to add
   */
  public void addComments(List<CommentDTO> commentDTOs) {
    commentDao.addComments(commentDTOs);
  }

  /**
   * Extracts comments from the given Java code and returns a list of CommentDTO objects.
   *
   * @param str the Java code from which to extract comments
   * @return a list of CommentDTO objects representing the extracted comments
   */
  public List<CommentDTO> extractComments(String str) {
    ParseResult<CompilationUnit> pr = parser.parse(str);

    //        just merge the items but do not need set.
    List<CommentDTO> commentList = new ArrayList<>();
    Optional<CommentsCollection> commentsCol = pr.getCommentsCollection();

    if (commentsCol.isPresent()) {
      CommentsCollection comments = commentsCol.get();

      if (!comments.getComments().isEmpty()) {
        Set<JavadocComment> javadocComments = comments.getJavadocComments();
        Set<BlockComment> blockComments = comments.getBlockComments();
        Set<LineComment> lineComments = comments.getLineComments();

        List<CommentDTO> javaDocStrComments = filterJavaDocComments(javadocComments);
        List<CommentDTO> blockStrComments = filterBlockComments(blockComments);
        List<CommentDTO> lineStrComments = filterLineComments(lineComments);

        commentList.addAll(javaDocStrComments);
        commentList.addAll(blockStrComments);
        commentList.addAll(lineStrComments);
      }
    }

    return commentList;
  }

  /**
   * Filters a set of Javadoc comments and returns a list of CommentDTO objects.
   *
   * @param javadocComments the set of Javadoc comments to filter
   * @return a list of CommentDTO objects representing the filtered comments
   */
  private List<CommentDTO> filterJavaDocComments(Set<JavadocComment> javadocComments) {

    List<CommentDTO> commentDTOs = new ArrayList<>();

    for (JavadocComment javaDocComment : javadocComments) {
      String javaDocContent = javaDocComment.getContent().strip();
      CommentDTO commentDTO = new CommentDTO(javaDocContent, true, CommentType.JAVADOC);
      if (containsSatdTags(javaDocContent)
          && !containsLicense(javaDocContent)
          && !isBlankString(javaDocContent)) {
        commentDTO.setFiltered(false);
      }

      commentDTOs.add(commentDTO);
    }

    return commentDTOs;
  }

  /**
   * Filters a set of BlockComment and creates CommentDTO objects for the non-filtered comments.
   *
   * @param blockComments a set of BlockComment to filter
   * @return a List of CommentDTO objects representing the non-filtered BlockComment objects
   */
  private List<CommentDTO> filterBlockComments(Set<BlockComment> blockComments) {
    List<CommentDTO> commentDTOs = new ArrayList<>();

    for (BlockComment blockComment : blockComments) {
      String blockContent = blockComment.getContent().strip();
      CommentDTO commentDTO = new CommentDTO(blockContent, true, CommentType.BLOCK);
      if (!containsLicense(blockContent)
          && !containsSourceCode(blockContent)
          && !isBlankString(blockContent)) {
        commentDTO.setFiltered(false);
      }

      commentDTOs.add(commentDTO);
    }

    return commentDTOs;
  }

  /**
   * Filters the line comments and creates a list of CommentDTO objects.
   *
   * @param lineComments a set of LineComment objects to be filtered
   * @return a list of CommentDTO objects containing the non-filtered line comments
   */
  private List<CommentDTO> filterLineComments(Set<LineComment> lineComments) {

    List<CommentDTO> commentDTOs = new ArrayList<>();

    int lastLineNumb = -1;
    StringBuilder groupedLineComments = new StringBuilder();

    int lineCountStart = 1;
    int i = 0;
    CommentDTO commentDTO;
    int currLineNumb;
    for (LineComment lineComment : lineComments) {

      String lineContent = lineComment.getContent().strip();
      commentDTO = new CommentDTO(lineContent, true, CommentType.LINE);

      if (isAutoGeneratedComment(lineContent)) {
        commentDTOs.add(commentDTO);
        continue;
      }

      currLineNumb =
          lineComment.getBegin().orElse(new Position(lineCountStart, lineCountStart)).line;
      if (i == 0) {
        lastLineNumb = currLineNumb - 1;
      }

      if (currLineNumb == lastLineNumb + 1) {
        groupedLineComments.append(" ").append(lineContent);
      } else {

        commentDTO = createCommentDTOFromConds(groupedLineComments);
        commentDTOs.add(commentDTO);

        groupedLineComments = new StringBuilder();
        groupedLineComments.append(lineContent);
      }
      lastLineNumb = currLineNumb;
      i += 1;
    }

    if (!groupedLineComments.isEmpty()) {
      commentDTO = createCommentDTOFromConds(groupedLineComments);
      commentDTOs.add(commentDTO);
    }

    return commentDTOs;
  }

  /**
   * Checks if the given comment contains any of the SATD tags ("FIXME", "XXX", or "TODO").
   *
   * @param comment the comment string to check
   * @return true if the comment contains any of the SATD tags, false otherwise
   */
  private boolean containsSatdTags(String comment) {
    String lowerCase = comment.toLowerCase();
    return lowerCase.contains("fixme") || lowerCase.contains("xxx") || lowerCase.contains("todo");
  }

  /**
   * Checks if the given comment string contains the word "license" or "copyright" in any case.
   *
   * @param comment the comment string to check for a license.
   * @return true if the comment contains a license, false otherwise.
   */
  private boolean containsLicense(String comment) {
    String lowerCase = comment.toLowerCase();
    return lowerCase.contains("license") || lowerCase.contains("copyright");
  }

  /**
   * Checks if the given comment contains any source code by matching against a regular expression
   * pattern that looks for common Java syntax for control structures, primitive data types, and
   * common Java class methods.
   *
   * @param comment the comment to check for source code
   * @return true if the comment contains source code, false otherwise
   */
  private boolean containsSourceCode(String comment) {
    String SOURCE_CODE_REGEX =
        "else\\s*\\{|"
            + "try\\s*\\{|"
            + "do\\s*\\{|"
            + "finally\\s*\\{|"
            + "if\\s*\\(|"
            + "for\\s*\\(|"
            + "while\\s*\\(|"
            + "switch\\s*\\(|"
            + "Long\\s*\\(|"
            + "Byte\\s*\\(|"
            + "Double\\s*\\(|"
            + "Float\\s*\\(|"
            + "Integer\\s*\\(|"
            + "Short\\s*\\(|"
            + "BigDecimal\\s*\\(|"
            + "BigInteger\\s*\\(|"
            + "Character\\s*\\(|"
            + "Boolean\\s*\\(|"
            + "String\\s*\\(|"
            + "assert\\s*\\(|"
            + "System\\.out.|"
            + "public\\s*void|"
            + "private\\s*static\\*final|"
            + "catch\\s*\\(";
    return Pattern.compile(SOURCE_CODE_REGEX).matcher(comment).find();
  }

  /**
   * Returns true if the provided comment string is blank, false otherwise.
   *
   * @param comment the comment string to check for blankness
   * @return true if the provided comment string is blank, false otherwise
   */
  private boolean isBlankString(String comment) {
    return comment.replace("\t", "").isBlank();
  }

  /**
   * Checks if the given comment is an IDE auto-generated comment.
   *
   * @param comment the comment to check
   * @return true if the comment is an auto-generated comment, false otherwise
   */
  private boolean isAutoGeneratedComment(String comment) {
    return comment.toLowerCase().contains("todo auto-generated");
  }

  /**
   * Creates a CommenDTO from the given StringBuilder containing grouped line comments.
   *
   * @param strBuilder the StringBuilder containing grouped line comments.
   * @return a CommentDTO object representing the grouped line comments.
   */
  private CommentDTO createCommentDTOFromConds(StringBuilder strBuilder) {
    String groupedLineStr = strBuilder.toString();
    CommentDTO commentDTO = new CommentDTO(groupedLineStr, true, CommentType.GROUPED_LINE);
    if (!containsSourceCode(groupedLineStr)
        && !isBlankString(groupedLineStr)
        && !containsLicense(groupedLineStr)) {
      commentDTO.setFiltered(false);
    }
    return commentDTO;
  }
}
