package uk.ac.cam.cl.dtg.segue.etl.validators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.etl.ContentAugmenter;
import uk.ac.cam.cl.dtg.segue.etl.IndexingContext;

/**
 * Validates content for structural errors:
 * - Type-specific validation via ContentTypeValidator registry
 * - Referential integrity checks
 * - Cross-content relationship validation.
 */
public class ContentValidator {
  private static final Logger log = LoggerFactory.getLogger(ContentValidator.class);
  private static final String CONTENT_LOG_PREFIX = "CONTENT - ";

  private final ContentAugmenter augmenter;
  private final List<ContentTypeValidator> validators;

  /**
   * Record for tracking content references across the index.
   */
  private record ContentReferenceMap(Set<String> expectedIds, Map<String, Set<Content>> incomingReferences) {
  }

  public ContentValidator(final GitDb database, final ContentAugmenter augmenter) {
    this.augmenter = augmenter;

    this.validators = List.of(
        new GeneralContentValidator(),
        new MediaContentValidator(database),
        new QuestionContentValidator(),
        new ChoiceQuestionContentValidator(),
        new NumericQuestionContentValidator(),
        new SymbolicQuestionContentValidator(),
        new ClozeQuestionContentValidator(),
        new EmailTemplateContentValidator(),
        new EventContentValidator()
    );
  }

  /**
   * Records all validation problems found in the content cache.
   *
   * @param sha     version SHA for file lookups
   * @param context indexing context containing cache and problem map
   */
  public void recordContentErrors(final String sha, final IndexingContext context) {
    // Flatten all content objects from the cache
    Set<Content> flattenedSet = flattenAllContent(context);

    // Run type-specific and structural validators
    flattenedSet.forEach(c -> validateContent(sha, c, context));

    // Check referential integrity and published-to-unpublished references
    Map<String, Content> contentById = buildContentIndex(flattenedSet);
    ContentReferenceMap referenceMap = buildReferenceMap(flattenedSet);
    recordMissingContentProblems(referenceMap.expectedIds(), contentById, referenceMap.incomingReferences(), context);
    recordPublishedToUnpublishedReferenceProblems(referenceMap.incomingReferences(), contentById, context);

    if (context.indexProblemCache().isEmpty()) {
      // Register a no-op style error to simplify application logic by ensuring there is always a content errors
      // index. Without at least one document the CONTENT_ERROR index is never created, so the orchestrator's
      // allContentTypesAreIndexedForVersion verification fails and a problem-free version is wrongly rejected.
      Content dummyContentRecord = new Content();
      dummyContentRecord.setCanonicalSourceFile("😎");
      context.registerProblem(dummyContentRecord, "No content errors!");
    }
  }

  /**
   * Flattens all content objects from the cache into a single set.
   */
  private Set<Content> flattenAllContent(final IndexingContext context) {
    return context.contentCache().values().stream()
        .flatMap(c -> augmenter.flattenContentObjects(c).stream())
        .collect(Collectors.toSet());
  }

  /**
   * Validates a single content object using all registered validators.
   *
   * <p>Each validator is isolated: an unexpected failure on one malformed content item (or one
   * validator) must not abort validation — and therefore indexing — of the entire version. The
   * failure is both logged and recorded as a content problem so it surfaces in the indexing report.
   */
  private void validateContent(final String sha, final Content content, final IndexingContext context) {
    validators.forEach(validator -> {
      try {
        validator.validate(sha, content, context);
      } catch (RuntimeException e) {
        log.warn(CONTENT_LOG_PREFIX + "Validator {} failed for content in file {}: {}",
            validator.getClass().getSimpleName(), content.getCanonicalSourceFile(), e.getMessage());
        context.registerProblem(content, "Validation could not be completed by "
            + validator.getClass().getSimpleName() + ". The following error occurred: " + e.getMessage());
      }
    });
  }

  /**
   * Builds an index mapping content IDs to their objects.
   * On duplicate IDs the first occurrence is kept (duplicates are reported separately at cache time).
   */
  private Map<String, Content> buildContentIndex(final Set<Content> contentSet) {
    // Keep the first occurrence on duplicate IDs (mirrors the "first wins" cache dedup). Without a
    // merge function, toMap throws IllegalStateException on any duplicate ID and aborts the whole
    // validation/indexing run.
    return contentSet.stream()
        .filter(c -> c.getId() != null)
        .collect(Collectors.toMap(Content::getId, c -> c, (existing, replacement) -> existing));
  }

  /**
   * Builds a reference map tracking which content references which.
   */
  private ContentReferenceMap buildReferenceMap(final Set<Content> contentSet) {
    Set<String> expectedIds = new HashSet<>();
    Map<String, Set<Content>> incomingReferences = new HashMap<>();

    contentSet.stream().filter(c -> c.getRelatedContent() != null).forEach(c -> {
      expectedIds.addAll(c.getRelatedContent());
      c.getRelatedContent().forEach(id -> incomingReferences.computeIfAbsent(id, k -> new HashSet<>()).add(c));
    });

    return new ContentReferenceMap(expectedIds, incomingReferences);
  }

  /**
   * Records problems for missing referenced content.
   */
  private void recordMissingContentProblems(final Set<String> expectedIds, final Map<String, Content> contentById,
                                            final Map<String, Set<Content>> incomingReferences,
                                            final IndexingContext context) {
    Set<String> missingContent = new HashSet<>(expectedIds);
    missingContent.removeAll(contentById.keySet());

    // Check if this ID exists as an augmented child ID
    missingContent
        .forEach(id -> incomingReferences.get(id)
            .forEach(src -> {
              List<String> augmentedMatches = contentById.keySet().stream()
                  .filter(k -> k.endsWith(Constants.ID_SEPARATOR + id))
                  .toList();
              String diagnosis = augmentedMatches.isEmpty() ? ""
                  : " (Note: Found augmented forms in index: " + augmentedMatches
                  + " — the reference may use a bare ID but the content was indexed as a child)";
              context.registerProblem(src, "The id '" + id + "' was referenced by "
                  + src.getCanonicalSourceFile() + " but the content with that "
                  + "ID cannot be found." + diagnosis);
            }));
    if (!missingContent.isEmpty()) {
      log.warn(CONTENT_LOG_PREFIX + "Referential integrity broken for ({}) related Content items. "
          + "The following ids are referenced but do not exist: {}", missingContent.size(), missingContent);
    }
  }

  /**
   * Records problems for published content referencing unpublished content.
   */
  private void recordPublishedToUnpublishedReferenceProblems(final Map<String, Set<Content>> incomingReferences,
                                                             final Map<String, Content> contentById,
                                                             final IndexingContext context) {
    for (Map.Entry<String, Set<Content>> entry : incomingReferences.entrySet()) {
      String refTargetId = entry.getKey();
      Content refTarget = contentById.get(refTargetId);
      if (refTarget != null) {
        entry.getValue().stream().filter(
                refSrc -> Boolean.TRUE.equals(refSrc.getPublished()) && !Boolean.TRUE.equals(refTarget.getPublished()))
            .forEach(refSrc -> context.registerProblem(refSrc, "Content is published, "
                + "but references unpublished content '" + refTargetId + "'."));
      }
    }
  }
}