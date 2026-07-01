package uk.ac.cam.cl.dtg.segue.etl.validators;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

class MediaContentValidatorTest {

  @Test
  void validate_withNonMediaContent_skipsValidation() {
    MediaContentValidator validator = new MediaContentValidator(null);

    Content content = new Content("content-1", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", content, context);

    assertTrue(problems.isEmpty());
  }

  @Test
  void validate_withImageContent_acceptsImageType() {
    MediaContentValidator validator = new MediaContentValidator(null);

    Content imageContent = new Content("image-1", "image", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "An image description", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    validator.validate("test-sha", imageContent, context);

    assertTrue(problems.isEmpty() || !problems.containsKey(imageContent));
  }

  @Test
  void constructor_acceptsGitDb() {
    MediaContentValidator validator = new MediaContentValidator(null);
    assertNotNull(validator);
  }
}
