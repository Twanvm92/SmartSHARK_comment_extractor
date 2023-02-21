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

public class CommentService {

  private final CommentDao commentDao;
  private final JavaParser parser;

  public CommentService(CommentDao commentDao) {
    this.commentDao = commentDao;
    parser = new JavaParser();
    parser.getParserConfiguration().setPreprocessUnicodeEscapes(true);
  }

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
              commentDTO.setFileActionId(fileActionId);
              commentDTO.setFileId(fileId);
              commentDTO.setFilePath(filePath);
            })
        .collect(Collectors.toList());
  }

  public void addComments(List<CommentDTO> commentDTOs) {
    commentDao.addComments(commentDTOs);
  }

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

  private List<CommentDTO> filterLineComments(Set<LineComment> lineComments) {

    List<CommentDTO> commentDTOs = new ArrayList<>();

    int lastLineNumb = -1;
    StringBuilder groupedLineComments = new StringBuilder();

    int lineCountStart = 1;
    int i = 0;
    CommentDTO commentDTO;
    int currLineNumb = 0;
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

  private boolean containsSatdTags(String comment) {
    String lowerCase = comment.toLowerCase();
    return lowerCase.contains("fixme") || lowerCase.contains("xxx") || lowerCase.contains("todo");
  }

  private boolean containsLicense(String comment) {
    String lowerCase = comment.toLowerCase();
    return lowerCase.contains("license") || lowerCase.contains("copyright");
  }

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

  private boolean isBlankString(String comment) {
    return comment.replace("\t", "").isBlank();
  }

  private boolean isAutoGeneratedComment(String comment) {
    return comment.toLowerCase().contains("todo auto-generated");
  }

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

  public void getAndAddDeduplicatedComments() {
    commentDao.getAndAddDeduplicatedComments();
  }
}
