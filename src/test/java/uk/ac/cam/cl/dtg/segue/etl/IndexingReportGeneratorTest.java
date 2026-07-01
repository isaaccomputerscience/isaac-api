package uk.ac.cam.cl.dtg.segue.etl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;

class IndexingReportGeneratorTest {

  @Test
  void generateIndexingReport_withEmptyContext_doesNotThrow() {
    
    IndexingReportGenerator reporter = new IndexingReportGenerator();
    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), new HashMap<>(), true);

    assertDoesNotThrow(() -> reporter.generateIndexingReport("test-sha", context));
  }

  @Test
  void generateIndexingReport_withSingleProblem_generatesReport() {
    
    IndexingReportGenerator reporter = new IndexingReportGenerator();

    Content content = new Content("test-id", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    List<String> errorList = new ArrayList<>();
    errorList.add("Test error message");
    problems.put(content, errorList);

    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    assertDoesNotThrow(() -> reporter.generateIndexingReport("test-sha", context));
  }

  @Test
  void generateIndexingReport_withMultipleProblems_generatesReport() {
    
    IndexingReportGenerator reporter = new IndexingReportGenerator();

    Content content1 = new Content("id-1", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);
    Content content2 = new Content("id-2", "question", "Question: ", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();

    List<String> errors1 = new ArrayList<>();
    errors1.add("Duplicate ID: id-1");
    errors1.add("Missing title");
    problems.put(content1, errors1);

    List<String> errors2 = new ArrayList<>();
    errors2.add("Missing correct answer");
    problems.put(content2, errors2);

    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    assertDoesNotThrow(() -> reporter.generateIndexingReport("test-sha", context));
  }

  @Test
  void generateIndexingReport_withDuplicateIdError_classifiesAsContentError() {
    
    IndexingReportGenerator reporter = new IndexingReportGenerator();

    Content content = new Content("dup-id", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    List<String> errorList = new ArrayList<>();
    errorList.add("Duplicate Content ID: dup-id");
    problems.put(content, errorList);

    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    assertDoesNotThrow(() -> reporter.generateIndexingReport("test-sha", context));
  }

  @Test
  void generateIndexingReport_withMediaError_classifiesAsMediaIssue() {
    
    IndexingReportGenerator reporter = new IndexingReportGenerator();

    Content media = new Content("media-1", "image", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    List<String> errorList = new ArrayList<>();
    errorList.add("Media alt text missing for /assets/test.jpg");
    problems.put(media, errorList);

    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    assertDoesNotThrow(() -> reporter.generateIndexingReport("test-sha", context));
  }

  @Test
  void generateIndexingReport_withQuestionError_classifiesAsQuestionIssue() {
    
    IndexingReportGenerator reporter = new IndexingReportGenerator();

    Content question = new Content("q-1", "isaacQuestion", "Question: ", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    List<String> errorList = new ArrayList<>();
    errorList.add("Question missing correct answer");
    problems.put(question, errorList);

    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    assertDoesNotThrow(() -> reporter.generateIndexingReport("test-sha", context));
  }

  @Test
  void generateIndexingReport_withEventError_classifiesAsEventIssue() {
    
    IndexingReportGenerator reporter = new IndexingReportGenerator();

    Content event = new Content("event-1", "isaacEventPage", "Event: ", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);

    Map<Content, List<String>> problems = new HashMap<>();
    List<String> errorList = new ArrayList<>();
    errorList.add("Event missing end date");
    problems.put(event, errorList);

    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    assertDoesNotThrow(() -> reporter.generateIndexingReport("test-sha", context));
  }

  @Test
  void generateIndexingReport_withMixedErrors_classifiesCorrectly() {
    
    IndexingReportGenerator reporter = new IndexingReportGenerator();

    Map<Content, List<String>> problems = new HashMap<>();

    Content content1 = new Content("id-1", "paragraph", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);
    List<String> errors1 = new ArrayList<>();
    errors1.add("Duplicate Content ID: id-1");
    problems.put(content1, errors1);

    Content content2 = new Content("media-1", "image", "test", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);
    List<String> errors2 = new ArrayList<>();
    errors2.add("Media alt text missing");
    problems.put(content2, errors2);

    Content content3 = new Content("q-1", "isaacQuestion", "Q:", "test", "test", "test", "test", "test",
        new LinkedList<>(), "test", "test", new LinkedList<>(), false, false, new HashSet<>(), 1);
    List<String> errors3 = new ArrayList<>();
    errors3.add("Question missing correct answer");
    problems.put(content3, errors3);

    IndexingContext context = new IndexingContext(new HashMap<>(), new HashSet<>(), new HashMap<>(),
        new HashMap<>(), problems, true);

    assertDoesNotThrow(() -> reporter.generateIndexingReport("test-sha", context));

    assertEquals(3, problems.size());
  }
}
