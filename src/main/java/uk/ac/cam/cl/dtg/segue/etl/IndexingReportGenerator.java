package uk.ac.cam.cl.dtg.segue.etl;

import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseInternalLogValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;

/**
 * Generates detailed indexing failure reports.
 */
public class IndexingReportGenerator {

  private static final Logger log = LoggerFactory.getLogger(IndexingReportGenerator.class);
  private static final String CONTENT_LOG_PREFIX = "CONTENT - ";

  /**
   * Generates and logs a detailed indexing report.
   */
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  public void generateIndexingReport(final String version, final IndexingContext context) {
    Map<Content, List<String>> indexProblemCache = context.indexProblemCache();
    Map<String, Content> contentCache = context.contentCache();

    if (indexProblemCache.isEmpty()) {
      log.info(CONTENT_LOG_PREFIX + "Indexing completed successfully with NO validation errors or warnings");
      return;
    }

    // Filter out dummy "no errors" record
    List<Map.Entry<Content, List<String>>> realProblems = indexProblemCache.entrySet().stream()
        .filter(e -> !e.getKey().getCanonicalSourceFile().equals("😎"))
        .toList();

    if (realProblems.isEmpty()) {
      log.info(CONTENT_LOG_PREFIX + "Indexing completed successfully with NO validation errors or warnings");
      return;
    }

    // Emit the report as individual single-line log events (each prefixed with CONTENT_LOG_PREFIX) so
    // that CloudWatch renders each line as its own readable, searchable entry rather than one large
    // multi-line blob with escaped newlines.
    log.warn(CONTENT_LOG_PREFIX + "===== INDEXING FAILURE REPORT START =====");
    log.warn(CONTENT_LOG_PREFIX + "RESULT - Version: {}", sanitiseInternalLogValue(version));
    log.warn(CONTENT_LOG_PREFIX + "RESULT - Successfully indexed: {} items", contentCache.size());
    log.warn(CONTENT_LOG_PREFIX + "RESULT - Items with problems: {} items", realProblems.size());

    // Group problems by error type and file
    Map<String, List<Map.Entry<Content, List<String>>>> problemsByType = groupProblems(realProblems);

    // Report each problem with details, one log event per line
    int problemIndex = 1;
    for (Map.Entry<String, List<Map.Entry<Content, List<String>>>> typeGroup : problemsByType.entrySet()) {
      log.warn(CONTENT_LOG_PREFIX + "[{}]", typeGroup.getKey());
      for (Map.Entry<Content, List<String>> problem : typeGroup.getValue()) {
        Content content = problem.getKey();

        log.warn(CONTENT_LOG_PREFIX + "{}. {} | id={} | title={} | type={} | published={}",
            problemIndex, content.getCanonicalSourceFile(), content.getId(), content.getTitle(),
            content.getType(), content.getPublished());

        for (String error : problem.getValue()) {
          log.warn(CONTENT_LOG_PREFIX + "RESULT - {}", error);
        }

        problemIndex++;
      }
    }

    // Summary by type
    log.warn(CONTENT_LOG_PREFIX + "SUMMARY BY ERROR TYPE:");
    for (Map.Entry<String, List<Map.Entry<Content, List<String>>>> typeGroup : problemsByType.entrySet()) {
      int totalIssues = typeGroup.getValue().stream()
          .mapToInt(e -> e.getValue().size())
          .sum();
      log.warn(CONTENT_LOG_PREFIX + "  {}: {} files, {} total issues",
          typeGroup.getKey(), typeGroup.getValue().size(), totalIssues);
    }

    log.warn(CONTENT_LOG_PREFIX + "===== INDEXING FAILURE REPORT END =====");
  }

  /**
   * Group problems by error type for organized reporting.
   * Classifies errors based on keyword matching in the first error message.
   *
   * @param problems the list of content with problems
   * @return a map of error type to list of problems
   */
  private Map<String, List<Map.Entry<Content, List<String>>>> groupProblems(
      final List<Map.Entry<Content, List<String>>> problems) {
    Map<String, List<Map.Entry<Content, List<String>>>> grouped = new LinkedHashMap<>();

    for (Map.Entry<Content, List<String>> problem : problems) {
      String category = classifyErrorCategory(problem.getValue());
      grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(problem);
    }

    return grouped;
  }

  /**
   * Classify error message into a category based on keywords.
   *
   * @param errors the error messages for a single content item
   * @return the error category
   */
  private String classifyErrorCategory(final List<String> errors) {
    if (errors == null || errors.isEmpty()) {
      return "Validation Failures";
    }

    String firstError = errors.get(0).toLowerCase();

    if (firstError.contains("duplicate") || firstError.contains("already indexed")) {
      return "Duplicate Content";
    } else if (firstError.contains("media") || firstError.contains("alt text") || firstError.contains("image")) {
      return "Media/Alt Text Issues";
    } else if (firstError.contains("choice") || firstError.contains("answer") || firstError.contains("question")) {
      return "Question/Choice Issues";
    } else if (firstError.contains("event") || firstError.contains("end date")) {
      return "Event Issues";
    } else if (firstError.contains("email") || firstError.contains("template")) {
      return "Email Template Issues";
    } else if (firstError.contains("index") || firstError.contains("failed")) {
      return "Indexing Errors";
    }

    return "Validation Failures";
  }
}