package uk.ac.cam.cl.dtg.segue.etl.validators;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

@SuppressWarnings("unchecked")

class GeneralContentValidatorTest {

  @Test
  void validate_withValidContent_registersNoProblems() {
    
    GeneralContentValidator validator = new GeneralContentValidator();
    Content content = new Content("test-id", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", content, context);

    assertTrue(problems.isEmpty());
  }

  // Content ID structural rules (null/empty, illegal '.' chars, max length) are enforced as hard gates at
  // cache time in ContentGitLoader#validateAndCacheContent, NOT by this validator. The validator must not
  // flag them, in particular it must not flag the many content blocks that legitimately have no ID.

  @Test
  void validate_withNullId_registersNoIdProblem() {
    GeneralContentValidator validator = new GeneralContentValidator();
    Content content = new Content(null, "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", content, context);

    assertTrue(problems.isEmpty());
  }

  @Test
  void validate_withIdContainingDot_registersNoProblem() {
    GeneralContentValidator validator = new GeneralContentValidator();
    Content content = new Content("test.id", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", content, context);

    assertTrue(problems.isEmpty());
  }

  @Test
  void validate_withIdTooLong_registersNoProblem() {
    GeneralContentValidator validator = new GeneralContentValidator();
    String longId = "a".repeat(600); // Exceeds MAXIMUM_CONTENT_ID_LENGTH (512)
    Content content = new Content(longId, "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", content, context);

    assertTrue(problems.isEmpty());
  }

  @Test
  void validate_withChildrenContainingNonContent_registersProblem() {
    
    GeneralContentValidator validator = new GeneralContentValidator();
    LinkedList<java.lang.Object> children = new LinkedList<>();
    children.add("not-a-content"); // Invalid child type

    Content content = new Content("test-id", "paragraph", "test", "test", "test", "test", "test", "test",
        (java.util.List<ContentBase>) (java.util.List<?>) children, "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    
    validator.validate("test-sha", content, context);

    assertTrue(problems.containsKey(content));
  }

  @Test
  void validate_withValidExpandable_registersNoProblems() {
    GeneralContentValidator validator = new GeneralContentValidator();

    Content content = new Content("test-id", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", content, context);

    assertTrue(problems.isEmpty() || !problems.containsKey(content));
  }
}
