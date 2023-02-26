import static org.mockito.Mockito.mock;

import com.github.javaparser.JavaParser;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.example.daos.CommentDao;
import org.example.daos.ConfigDAO;
import org.example.daos.ProjectDao;
import org.example.models.CommentDTO;
import org.example.services.CommentService;
import org.example.services.HunkService;
import org.example.services.ProjectService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommentServiceTest {

  private CommentService commentService;
  private JavaParser javaParser;
  private ProjectService projectService;

  @BeforeEach
  void setUp() {
    CommentDao commentDao = mock(CommentDao.class);
    ConfigDAO configDao = mock(ConfigDAO.class);
    ProjectDao projectDao = mock(ProjectDao.class);
    HunkService hunkService = mock(HunkService.class);
    javaParser = new JavaParser();
    javaParser.getParserConfiguration().setPreprocessUnicodeEscapes(true);
    commentService = new CommentService(commentDao);
    projectService = new ProjectService(projectDao, hunkService, configDao, commentService);
  }

  @Test
  void name() throws IOException {
    /*
     get original hunk context from aggregator on original hunk collection matching on a specific
     hunk id
    */
    FileInputStream fis = new FileInputStream("src/test/resources/testString.txt");
    //    screws up because of carriage return
    String codeStr = IOUtils.toString(fis, StandardCharsets.UTF_8);
    List<String> addedLinesGroups = projectService.extractAddedLinesFromContent(codeStr);

    //    get comment content extracted from the specific hunk from the twan_satd database on the
    // deduplicated_studio3t collection using the id in the python df.
    String expComment =
        "the button name (i.e. 'bold') the UI element (DIV) is it enabled? is it pressed? enabled in text mode? the command ID for changing state";
    boolean expCommentFound = false;
    for (int i = 0; i < addedLinesGroups.size(); i++) {
      List<CommentDTO> commentDTOS = commentService.extractComments(addedLinesGroups.get(i));
      for (int j = 0; j < commentDTOS.size(); j++) {
        if (commentDTOS.get(j).getContent().equals(expComment)) {
          expCommentFound = true;
          break;
        }
      }
    }

    //    TODO code from chatgpt using the following question: "check if LineComment in JavaParser
    // starts after code on the same line" to check line start and end from code on same line.
    //    here it says > line though but line numbers should be same but col start of comment
    //    should be higher than col end of code!
    //
    //    // Assuming you have parsed the code and obtained a `CompilationUnit` object
    //    for (Comment comment : cu.getAllContainedComments()) {
    //      if (comment instanceof LineComment) {
    //        int commentStartLine = comment.getBegin().get().line;
    //        int codeEndLine = comment.getCommentedNode().get().getEnd().get().line;
    //        boolean startsAfterCodeOnSameLine = (commentStartLine > codeEndLine);
    //        System.out.println("Comment starts after code on the same line: " +
    // startsAfterCodeOnSameLine);
    //      }
    //    }

    Assertions.assertTrue(expCommentFound);
  }
}
