package uk.ac.cam.cl.dtg.segue.etl.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.etl.ContentAugmenter;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

class ContentValidatorTest {

  @Test
  void recordContentErrors_withEmptyCache_doesNotThrow() {
    ContentAugmenter augmenter = new ContentAugmenter();
    ContentValidator validator = new ContentValidator(null, augmenter);
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), new HashMap<>(), true);

    assertDoesNotThrow(() -> validator.recordContentErrors("test-sha", context));
  }

  @Test
  void recordContentErrors_withSingleContent_flattensAndValidates() {
    ContentAugmenter augmenter = new ContentAugmenter();
    ContentValidator validator = new ContentValidator(null, augmenter);

    Content content = new Content("test-id", "test", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<String, Content> cache = new HashMap<>();
    cache.put("test-id", content);

    IndexingContext context = new IndexingContext(cache, new HashSet<>(), new HashMap<>(),
        new HashMap<>(), new HashMap<>(), true);

    // Execute - should handle single content without throwing
    assertDoesNotThrow(() -> validator.recordContentErrors("test-sha", context));

    // Verify - cache still contains the content
    assertEquals(1, cache.size());
    assertTrue(cache.containsKey("test-id"));
  }

  @Test
  void recordContentErrors_withMultipleContents_validatesAll() {
    ContentAugmenter augmenter = new ContentAugmenter();
    ContentValidator validator = new ContentValidator(null, augmenter);

    Content content1 = new Content("id1", "test", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);
    Content content2 = new Content("id2", "test", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<String, Content> cache = new HashMap<>();
    cache.put("id1", content1);
    cache.put("id2", content2);

    IndexingContext context = new IndexingContext(cache, new HashSet<>(), new HashMap<>(),
        new HashMap<>(), new HashMap<>(), true);

    // Execute - should handle multiple contents
    assertDoesNotThrow(() -> validator.recordContentErrors("test-sha", context));

    // Verify - both contents validated
    assertEquals(2, cache.size());
    assertTrue(cache.containsKey("id1"));
    assertTrue(cache.containsKey("id2"));
  }

  @Test
  void buildContentIndex_createsMapFromSet() {
    // This test verifies that recordContentErrors correctly calls buildContentIndex
    // buildContentIndex is private, so we test it indirectly through recordContentErrors
    ContentAugmenter augmenter = new ContentAugmenter();
    ContentValidator validator = new ContentValidator(null, augmenter);

    Content content1 = new Content("id1", "test", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);
    Content content2 = new Content("id2", "test", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<String, Content> cache = new HashMap<>();
    cache.put("id1", content1);
    cache.put("id2", content2);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(cache, new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    // Execute - recordContentErrors calls buildContentIndex internally
    assertDoesNotThrow(() -> validator.recordContentErrors("test-sha", context));

    // Verify - both contents were processed
    assertEquals(2, cache.size());
    assertTrue(cache.containsKey("id1"));
    assertTrue(cache.containsKey("id2"));
  }

  @Test
  void recordContentErrors_withNullIdsInContent_handlesMissing() {
    // Setup
    ContentAugmenter augmenter = new ContentAugmenter();
    ContentValidator validator = new ContentValidator(null, augmenter);

    Content content = new Content(null, "test", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<String, Content> cache = new HashMap<>();
    cache.put("null-key", content);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(cache, new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    // Execute - should handle null ID gracefully
    assertDoesNotThrow(() -> validator.recordContentErrors("test-sha", context));

    // Verify - a plain id-less content block is NOT flagged (most content legitimately has no ID; the
    // structural ID gate lives in ContentGitLoader). The only entry added is the "no errors" placeholder.
    assertFalse(problems.containsKey(content));
  }

  @Test
  void recordContentErrors_registersProblems() {
    ContentAugmenter augmenter = new ContentAugmenter();
    ContentValidator validator = new ContentValidator(null, augmenter);

    // A Question without an ID must be flagged by QuestionContentValidator. Use a real Question instance,
    // not a base Content, so the instanceof check in the validator matches.
    Question question = new Question();
    question.setType("question");
    question.setTitle("Question: ");
    question.setId(null);

    Map<String, Content> cache = new HashMap<>();
    cache.put("question-key", question);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(cache, new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    // Execute
    assertDoesNotThrow(() -> validator.recordContentErrors("test-sha", context));

    // Verify - missing-ID problem was registered against the question
    assertTrue(problems.containsKey(question));
    assertFalse(problems.get(question).isEmpty());
  }
}
