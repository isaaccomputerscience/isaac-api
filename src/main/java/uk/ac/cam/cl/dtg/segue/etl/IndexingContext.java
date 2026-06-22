package uk.ac.cam.cl.dtg.segue.etl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;

/**
 * Encapsulates the shared data structures passed through the content indexing pipeline.
 * Carries the in-memory cache, tags, units, problem registry, and metadata flags.
 * This is a record to automatically generate getters for all fields.
 */
public record IndexingContext(
    Map<String, Content> contentCache,
    Set<String> tagsList,
    Map<String, String> allUnits,
    Map<String, String> publishedUnits,
    Map<Content, List<String>> indexProblemCache,
    boolean includeUnpublished) {

  /**
   * Creates a fresh IndexingContext for a given version with empty collections.
   *
   * @param includeUnpublished whether to index unpublished content
   * @return new context with empty maps and sets
   */
  public static IndexingContext forVersion(boolean includeUnpublished) {
    return new IndexingContext(
        new HashMap<>(),
        new HashSet<>(),
        new HashMap<>(),
        new HashMap<>(),
        new HashMap<>(),
        includeUnpublished);
  }

  /**
   * Determines if unpublished content should be skipped for this content object.
   *
   * @param content the content to check
   * @return true if content should be skipped (not published and not including unpublished)
   */
  public boolean shouldSkipUnpublished(final Content content) {
    return !includeUnpublished && !content.getPublished();
  }

  /**
   * Registers a validation problem for the given content.
   *
   * @param content the content with the problem
   * @param message the problem description
   */
  public void registerProblem(final Content content, final String message) {
    indexProblemCache.computeIfAbsent(content, k -> new java.util.ArrayList<>()).add(message);
  }

}
