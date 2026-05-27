package uk.ac.cam.cl.dtg.segue.etl;

import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Objects.requireNonNull;
import static uk.ac.cam.cl.dtg.segue.api.Constants.BYTES_IN_ONE_KILOBYTE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ContentIndextype;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MAXIMUM_CONTENT_ID_LENGTH;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseInternalLogValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.elasticsearch.action.ActionRequestValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCardDeck;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacClozeQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuizSection;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.EmailTemplate;
import uk.ac.cam.cl.dtg.isaac.dos.content.Formula;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Media;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.content.Video;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

public class ContentIndexer {
  private static final Logger log = LoggerFactory.getLogger(ContentIndexer.class);

  private static final ConcurrentHashMap<String, Boolean> VERSION_LOCKS = new ConcurrentHashMap<>();
  private static final String QUESTION = "Question: ";
  private static final String NUMERIC_QUESTION = "Numeric Question: ";
  private static final String SYMBOLIC_QUESTION = "Symbolic Question: ";

  private final ElasticSearchIndexer es;
  private final GitDb database;
  private final ContentMapperUtils mapperUtils;

  private static final int MEDIA_FILE_SIZE_LIMIT = 300 * 1024; // Bytes
  private static final int NANOSECONDS_IN_A_MILLISECOND = 1000000;
  private static final String ERROR_OCCURRED_SUFFIX = ". The following error occurred: ";
  private static final String CONTENT_LOG_PREFIX = "CONTENT - ";


  @Inject
  public ContentIndexer(final GitDb database, final ElasticSearchIndexer es, final ContentMapperUtils mapperUtils) {
    this.database = database;
    this.es = es;
    this.mapperUtils = mapperUtils;
  }

  private void timeOperation(final String operationName, final CheckedOperation operation) throws Exception {
    long start = System.nanoTime();
    operation.execute();
    long durationMs = (System.nanoTime() - start) / NANOSECONDS_IN_A_MILLISECOND;
    log.info(CONTENT_LOG_PREFIX + "Finished {}, took: {}ms", operationName, durationMs);
  }

  void loadAndIndexContent(final String version) throws Exception {

    // Take version lock or fail
    Boolean alreadyLocked = VERSION_LOCKS.putIfAbsent(version, true);

    if (Boolean.TRUE.equals(alreadyLocked)) {
      throw new VersionLockedException(version);
    }

    log.info(CONTENT_LOG_PREFIX + "Acquired lock for version {}. Starting indexing.",
        sanitiseInternalLogValue(version));

    try {
      database.fetchLatestFromRemote();

      // Now we have acquired the lock check in case someone else has already indexed this version.
      // The case where only some of the content types have been successfully indexed for this version,
      // should never happen but is covered by an expunge at the start of #buildElasticSearchIndex(...).
      if (allContentTypesAreIndexedForVersion(version)) {
        log.info(CONTENT_LOG_PREFIX + "Content already indexed: {}", sanitiseInternalLogValue(version));
        return;
      }

      log.info(CONTENT_LOG_PREFIX + "Rebuilding content index for sha: {}",
          sanitiseInternalLogValue(version));

      Map<String, Content> contentCache = new HashMap<>();
      Set<String> tagsList = new HashSet<>();
      Map<String, String> allUnits = new HashMap<>();
      Map<String, String> publishedUnits = new HashMap<>();
      Map<Content, List<String>> indexProblemCache = new HashMap<>();

      long totalStartTime = System.nanoTime();

      timeOperation("populating Git content cache", () ->
          buildGitContentIndex(version, true, contentCache, tagsList, allUnits, publishedUnits,
              indexProblemCache));
      log.info(CONTENT_LOG_PREFIX + "Beginning to record content errors");

      timeOperation("recording content errors", () ->
          recordContentErrors(version, contentCache, indexProblemCache));

      timeOperation("building ElasticSearch index", () ->
          buildElasticSearchIndex(version, contentCache, tagsList, allUnits, publishedUnits,
              indexProblemCache));

      // Verify the version requested is now available
      if (!allContentTypesAreIndexedForVersion(version)) {
        expungeAnyContentTypeIndicesRelatedToVersion(version);
        throw new VersionIndexingFailedException(version);
      }

      long totalTime = (System.nanoTime() - totalStartTime) / NANOSECONDS_IN_A_MILLISECOND;
      log.info(CONTENT_LOG_PREFIX + "Finished indexing version {}, total time: {}ms",
          sanitiseInternalLogValue(version), totalTime);

    } finally {
      VERSION_LOCKS.remove(version);
    }

  }

  void setNamedVersion(final String alias, final String version) {
    List<String> allContentTypes = Arrays.stream(ContentIndextype.values())
        .map(ContentIndextype::toString).toList();
    es.addOrMoveIndexAlias(alias, version, allContentTypes);
  }

  /**
   * This method will populate the internal gitCache based on the content object files found for a given SHA.
   * <br>
   * Currently, it only looks for json files in the repository.
   *
   * @param sha                the version to index.
   * @param includeUnpublished boolean controlling if unpublished content should be indexed
   * @param contentCache       a map of keys to content objects
   * @param tagsList           a set of seen tags
   * @param allUnits           a map of units used in numeric questions
   * @param publishedUnits     a map of units used in published numeric questions
   * @param indexProblemCache  a map of problems found in the indexed content
   * @throws ContentManagerException if the SHA is null or the associated resource cannot be accessed
   */
  private synchronized void buildGitContentIndex(final String sha,
                                                 final boolean includeUnpublished,
                                                 final Map<String, Content> contentCache,
                                                 final Set<String> tagsList,
                                                 final Map<String, String> allUnits,
                                                 final Map<String, String> publishedUnits,
                                                 final Map<Content, List<String>> indexProblemCache)
      throws ContentManagerException {

    if (null == sha) {
      throw new ContentManagerException("SHA is null. Cannot index.");
    }

    Repository repository = database.getGitRepository();

    try {
      ObjectId commitId = repository.resolve(sha);

      if (null == commitId) {
        throw new ContentManagerException("Failed to buildGitIndex - Unable to locate resource with SHA: "
            + sha);
      }

      TreeWalk treeWalk = database.getTreeWalk(sha, ".json");

      if (null == treeWalk) {
        throw new ContentManagerException("Failed to buildGitIndex - Unable to get tree walk for SHA: " + sha);
      }

      log.info("Populating git content cache based on sha {} ...", sanitiseInternalLogValue(sha));

      // Traverse the git repository looking for the .json files
      IndexingContext context = new IndexingContext(contentCache, tagsList, allUnits, publishedUnits,
          indexProblemCache, includeUnpublished);
      while (treeWalk.next()) {
        processJsonFile(treeWalk, repository, context);
      }

      repository.close();
      log.info("Tags available {}", tagsList);
      log.info("All units: {}", allUnits);

    } catch (IOException e) {
      log.error("IOException while trying to access git repository. ", e);
      throw new ContentManagerException("Unable to index content, due to an IOException.");
    }
  }

  private void processJsonFile(final TreeWalk treeWalk, final Repository repository,
                               final IndexingContext context) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
      loader.copyTo(out);

      ObjectMapper objectMapper = mapperUtils.getSharedContentObjectMapper();

      try {
        Content content = (Content) objectMapper.readValue(out.toString(), ContentBase.class);

        if (context.shouldSkipUnpublished(content)) {
          log.info("Skipping unpublished content: {}", content.getId());
          return;
        }

        content = augmentChildContent(content, treeWalk.getPathString(), null, content.getPublished());

        if (null != content) {
          log.info(CONTENT_LOG_PREFIX + "Processing file: {} (type: {}, id: {})", treeWalk.getPathString(),
              content.getType(), content.getId());
          indexContentObject(context.contentCache(), context.tagsList(), context.allUnits(), context.publishedUnits(),
              context.indexProblemCache(), treeWalk.getPathString(), content);
        }
      } catch (JsonMappingException e) {
        log.warn(CONTENT_LOG_PREFIX + "Unable to parse the json file found {} as a content object. "
            + "Skipping file due to error: \n {}", treeWalk.getPathString(), e.getMessage());
        registerContentProblem(createErrorDummyContent(treeWalk.getPathString()),
            "Index failure - Unable to parse json file found - "
            + treeWalk.getPathString() + ERROR_OCCURRED_SUFFIX + e.getMessage(), context.indexProblemCache());
      } catch (IOException e) {
        log.error("IOException while trying to parse {}", treeWalk.getPathString(), e);
        registerContentProblem(createErrorDummyContent(treeWalk.getPathString()),
            "Index failure - Unable to read the json file found - " + treeWalk.getPathString()
                + ERROR_OCCURRED_SUFFIX + e.getMessage(), context.indexProblemCache());
      }
    } catch (Exception e) {
      log.error(CONTENT_LOG_PREFIX
          + "Unexpected error while processing file {}: {}", treeWalk.getPathString(), e.getMessage(), e);
      registerContentProblem(createErrorDummyContent(treeWalk.getPathString()),
          "Index failure - Unexpected error while processing file - " + treeWalk.getPathString()
              + ERROR_OCCURRED_SUFFIX + e.getMessage(), context.indexProblemCache());
    }
  }

  private Content createErrorDummyContent(final String path) {
    Content dummy = new Content();
    dummy.setCanonicalSourceFile(path);
    return dummy;
  }

  private void indexContentObject(
      final Map<String, Content> contentCache, final Set<String> tagsList, final Map<String, String> allUnits,
      final Map<String, String> publishedUnits, final Map<Content, List<String>> indexProblemCache,
      final String treeWalkPath,
      final Content content) {
    // Walk the content for site-wide searchable fields
    StringBuilder searchableContentBuilder = new StringBuilder();
    collateSearchableContent(content, searchableContentBuilder);
    content.setSearchableContent(searchableContentBuilder.toString());

    // add children (and parent) from flattened Set to
    // cache if they have ids
    IndexingContext context = new IndexingContext(contentCache, tagsList, allUnits, publishedUnits,
        indexProblemCache, true);
    flattenContentObjects(content).forEach(flattenedContent ->
        validateAndCacheContent(flattenedContent, treeWalkPath, context));
  }

  private void validateAndCacheContent(final Content flattenedContent, final String treeWalkPath,
                                       final IndexingContext context) {
    if (flattenedContent.getId() == null) {
      return;
    }

    if (flattenedContent instanceof IsaacQuiz) {
      List<ContentBase> children = flattenedContent.getChildren();
      if (children != null && children.stream().anyMatch(c -> !(c instanceof IsaacQuizSection))) {
        log.info("IsaacQuiz ({}) contains top-level non-quiz sections. Skipping.", flattenedContent.getId());
        this.registerContentProblem(flattenedContent, "Index failure - Invalid "
            + "content type among quiz sections. Quizzes can only contain quiz sections "
            + "in the top-level children array.", context.indexProblemCache());
        return;
      }
    }

    if (flattenedContent.getId().length() > MAXIMUM_CONTENT_ID_LENGTH) {
      log.info("Content ID too long: {}", flattenedContent.getId());
      this.registerContentProblem(flattenedContent, "Content ID too long: " + flattenedContent.getId(),
          context.indexProblemCache());
      return;
    }

    if (flattenedContent.getId().contains(".")) {
      log.info("Resource with invalid ID ({}) detected in cache. Skipping {}", flattenedContent.getId(), treeWalkPath);
      this.registerContentProblem(flattenedContent, "Index failure - Invalid ID "
          + flattenedContent.getId() + " found in file " + treeWalkPath
          + ". Must not contain restricted characters.", context.indexProblemCache());
      return;
    }

    if (!context.contentCache().containsKey(flattenedContent.getId())) {
      context.contentCache().put(flattenedContent.getId(), flattenedContent);
      log.info(CONTENT_LOG_PREFIX + "Cached content: {} (type: {})", flattenedContent.getId(),
          flattenedContent.getType());
      registerTags(flattenedContent.getTags(), context.tagsList());

      if (flattenedContent instanceof IsaacNumericQuestion isaacNumericQuestion) {
        registerUnits(isaacNumericQuestion, context.allUnits(), context.publishedUnits());
      }
      return;
    }

    if (context.contentCache().get(flattenedContent.getId()).equals(flattenedContent)) {
      log.info("Resource ({}) already seen in cache. Skipping {}", parentContent.getId(), treeWalkPath);
      return;
    }

    log.warn("Resource with duplicate ID ({}) detected in cache. Re-indexing with latest version from {}",
        flattenedContent.getId(), treeWalkPath);
    context.contentCache().put(flattenedContent.getId(), flattenedContent);
    this.registerContentProblem(flattenedContent, String.format(
            "Duplicate ID (%s) found in files (%s) and (%s): using latest version (%s).",
            flattenedContent.getId(),
            context.contentCache().get(flattenedContent.getId()).getCanonicalSourceFile(),
            treeWalkPath,
            treeWalkPath),
        context.indexProblemCache());
  }

  private String computeParentId(final String parentId, final String contentId) {
    if (parentId == null && contentId != null) {
      return contentId;
    }
    if (contentId != null) {
      return parentId + Constants.ID_SEPARATOR + contentId;
    }
    return parentId;
  }

  /**
   * Augments all child objects recursively to include additional information.
   * <br>
   * This should be done before saving to the local gitCache in memory storage.
   * <br>
   * This method will also attempt to reconstruct object id's of nested content such that they are unique to the page
   * by default.
   *
   * @param content             content to augment
   * @param canonicalSourceFile source file to add to child content
   * @param parentId            used to construct nested ids for child elements
   * @param parentPublished     boolean to set published state based on parent state
   * @return Content object with new reference
   */
  private Content augmentChildContent(final Content content, final String canonicalSourceFile,
                                      @Nullable final String parentId, final boolean parentPublished) {
    if (null == content) {
      return null;
    }

    // If this object is of type question then we need to give it a random
    // id if it doesn't have one.
    if (content instanceof Question && content.getId() == null) {
      log.info("Found question without id {} {}", content.getTitle(), canonicalSourceFile);
    }

    String newParentId = computeParentId(parentId, content.getId());
    content.setCanonicalSourceFile(canonicalSourceFile);

    if (content.getChildren() != null && !content.getChildren().isEmpty()) {
      content.getChildren().stream().filter(Content.class::isInstance).map(cb -> (Content) cb)
          .forEach(c -> this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished));
    }

    if (content instanceof Choice choice) {
      this.augmentChildContent((Content) choice.getExplanation(), canonicalSourceFile,
          newParentId, parentPublished);
    }

    if (content instanceof IsaacCardDeck isaacCardDeck && isaacCardDeck.getCards() != null) {
      isaacCardDeck.getCards().forEach(card -> this.augmentChildContent(card, canonicalSourceFile,
          newParentId, parentPublished));
    }

    if (content instanceof Question question) {
      augmentQuestionContent(question, canonicalSourceFile, newParentId, parentPublished);
    }

    augmentMediaFieldsViaReflection(content, canonicalSourceFile);
    augmentMediaContent(content, canonicalSourceFile, parentId);
    updateContentIdentifier(content, newParentId, parentPublished);

    return content;
  }

  private void augmentMediaContent(final Content content, final String canonicalSourceFile, final String parentId) {
    if (!(content instanceof Media media)) {
      return;
    }
    media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));
    if (media.getId() == null && media.getSrc() != null && parentId != null) {
      media.setId(parentId + Constants.ID_SEPARATOR
          + Base64.encodeBase64String(media.getSrc().getBytes()));
    }
  }

  private void updateContentIdentifier(final Content content, final String parentId, final boolean parentPublished) {
    if (content.getId() != null && parentId != null) {
      content.setId(parentId);
      content.setPublished(parentPublished);
    }
  }

  private void collateSearchableContent(final Content content, final StringBuilder searchableContentBuilder) {
    if (content == null) {
      return;
    }
    if (content.getTitle() != null) {
      searchableContentBuilder.append(content.getTitle()).append("\n");
    }
    if (content.getValue() != null) {
      searchableContentBuilder.append(content.getValue()).append("\n");
    }
    if (content.getChildren() != null) {
      content.getChildren().stream()
          .filter(Content.class::isInstance)
          .map(Content.class::cast)
          .forEach(child -> this.collateSearchableContent(child, searchableContentBuilder));
    }
  }

  private void augmentMediaFieldsViaReflection(final Content content, final String canonicalSourceFile) {
    Method[] methods = content.getClass().getDeclaredMethods();
    Arrays.stream(methods)
        .filter(method -> Media.class.isAssignableFrom(method.getReturnType()))
        .forEach(method -> {
          try {
            Media media = (Media) method.invoke(content);
            if (media != null) {
              media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));
            }
          } catch (SecurityException | IllegalAccessException | IllegalArgumentException
                   | InvocationTargetException e) {
            log.error("Unable to access method using reflection: attempting to fix Media Src", e);
          }
        });
  }

  /**
   * Augments question content by recursively processing hints, answers, feedback, and choices.
   *
   * @param question            the question to augment
   * @param canonicalSourceFile the canonical path for child content
   * @param newParentId         the parent ID for nested content IDs
   * @param parentPublished     the published state for nested content
   */
  private void augmentQuestionContent(final Question question, final String canonicalSourceFile,
                                      final String newParentId, final boolean parentPublished) {
    augmentHints(question, canonicalSourceFile, newParentId, parentPublished);
    augmentAnswerContent(question, canonicalSourceFile, newParentId, parentPublished);
    augmentFeedbackContent(question, canonicalSourceFile, newParentId, parentPublished);
    augmentChoiceQuestionContent(question, canonicalSourceFile, newParentId, parentPublished);
  }

  private void augmentHints(final Question question, final String canonicalSourceFile, final String newParentId,
                            final boolean parentPublished) {
    if (question.getHints() == null) {
      return;
    }
    question.getHints().stream().map(cb -> (Content) cb)
        .forEach(c -> this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished));
  }

  private void augmentAnswerContent(final Question question, final String canonicalSourceFile, final String newParentId,
                                    final boolean parentPublished) {
    if (question.getAnswer() instanceof Content answer && answer.getChildren() != null) {
      answer.getChildren().stream().map(cb -> (Content) cb)
          .forEach(c -> this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished));
    }
  }

  private void augmentFeedbackContent(final Question question,
                                      final String canonicalSourceFile,
                                      final String newParentId,
                                      final boolean parentPublished) {
    Content defaultFeedback = question.getDefaultFeedback();
    if (defaultFeedback != null && defaultFeedback.getChildren() != null) {
      defaultFeedback.getChildren().stream().map(cb -> (Content) cb)
          .forEach(c -> this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished));
    }
  }

  private void augmentChoiceQuestionContent(final Question question, final String canonicalSourceFile,
                                            final String newParentId, final boolean parentPublished) {
    if (question instanceof ChoiceQuestion choiceQuestion && choiceQuestion.getChoices() != null) {
      choiceQuestion.getChoices().stream().map(cb -> (Content) cb)
          .forEach(c -> this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished));
    }
  }

  private String fixMediaSrc(final String canonicalSourceFile, final String originalSrc) {
    if (originalSrc != null && !(originalSrc.startsWith("http://") || originalSrc.startsWith("https://")
        || originalSrc.startsWith("/assets/"))) {
      return FilenameUtils.normalize(FilenameUtils.getPath(canonicalSourceFile) + originalSrc, true);
    }
    return originalSrc;
  }

  /**
   * Unpack the content objects into one big set. Useful for validation but could produce a very large set
   *
   * @param content content object to flatten
   * @return Set of content objects comprised of all children and the parent.
   */
  public Set<Content> flattenContentObjects(final Content content) {
    Set<Content> result = new HashSet<>();
    result.add(content);

    if (content.getChildren() != null) {
      content.getChildren().stream()
          .filter(Content.class::isInstance)
          .map(Content.class::cast)
          .forEach(child -> result.addAll(flattenContentObjects(child)));
    }

    if (content instanceof IsaacCardDeck deck && deck.getCards() != null) {
      deck.getCards().forEach(card -> result.addAll(flattenContentObjects(card)));
    }

    return result;
  }

  /**
   * Register a validation problem found during content indexing.
   *
   * @param content           the content object with problems (must not be null)
   * @param message           error message describing the validation problem
   * @param indexProblemCache map accumulating problems indexed by content object
   */
  private synchronized void registerContentProblem(final Content content, final String message,
                                                   final Map<Content, List<String>> indexProblemCache) {
    requireNonNull(content, "content must not be null");
    ensureTitleExists(content);
    indexProblemCache.computeIfAbsent(content, c -> new ArrayList<>()).add(message);
    log.warn(CONTENT_LOG_PREFIX + "{}", message);
  }

  private void ensureTitleExists(final Content content) {
    if (content.getTitle() == null) {
      content.setTitle(Paths.get(content.getCanonicalSourceFile()).getFileName().toString());
    }
  }

  /**
   * Helper function to build up a set of used tags for each version.
   *
   * @param tags     set of tags to register.
   * @param tagsList a set of seen tags
   */
  private synchronized void registerTags(final Set<String> tags, final Set<String> tagsList) {

    if (null == tags || tags.isEmpty()) {
      // don't do anything.
      return;
    }
    // sanity check that tags are trimmed.
    tagsList.addAll(tags.stream().map(String::trim).collect(Collectors.toSet()));
  }

  /**
   * Helper function to accumulate the set of all units used in numeric question answers.
   *
   * @param q              numeric question from which to extract units.
   * @param allUnits       a map of units used in numeric questions
   * @param publishedUnits a map of units used in published numeric questions
   */
  private synchronized void registerUnits(final IsaacNumericQuestion q, final Map<String, String> allUnits,
                                          final Map<String, String> publishedUnits) {
    Map<String, String> newUnits = q.getChoices().stream()
        .filter(Quantity.class::isInstance)
        .map(Quantity.class::cast)
        .filter(quantity -> quantity.getUnits() != null && !quantity.getUnits().isEmpty())
        .collect(Collectors.toMap(
            quantity -> quantity.getUnits().replace("\t", "").replace("\n", "").replace(" ", ""),
            Quantity::getUnits));

    if (newUnits.isEmpty()) {
      return;
    }

    allUnits.putAll(newUnits);
    if (Boolean.TRUE.equals(q.getPublished())) {
      publishedUnits.putAll(newUnits);
    }
  }

  /**
   * This method will send off the information in the git cache to the search provider for indexing.
   *
   * @param sha               the version in the git cache to send to the search provider.
   * @param gitCache          a map that represents indexed content for a given sha.
   * @param tagsList          a set of seen tags
   * @param allUnits          a map of units used in numeric questions
   * @param publishedUnits    a map of units used in published numeric questions
   * @param indexProblemCache a map of problems found in the indexed content
   */
  public synchronized void buildElasticSearchIndex(final String sha,
                                                   final Map<String, Content> gitCache,
                                                   final Set<String> tagsList,
                                                   final Map<String, String> allUnits,
                                                   final Map<String, String> publishedUnits,
                                                   final Map<Content, List<String>> indexProblemCache) {
    if (anyContentTypesAreIndexedForVersion(sha)) {
      expungeAnyContentTypeIndicesRelatedToVersion(sha);
    }

    log.info(CONTENT_LOG_PREFIX + "Building search indexes for version: {}", sanitiseInternalLogValue(sha));
    log.info(CONTENT_LOG_PREFIX + "Content cache size: {} items", gitCache.size());
    log.info(CONTENT_LOG_PREFIX + "Units to index: {} total, {} published", allUnits.size(), publishedUnits.size());

    // setup object mapper to use pre-configured deserializer module.
    // Required to deal with type polymorphism
    ObjectMapper objectMapper = mapperUtils.getSharedContentObjectMapper();
    List<Map.Entry<String, String>> contentToIndex = gitCache.values().stream()
        .flatMap(content -> {
          try {
            return Stream.of(immutableEntry(content.getId(), objectMapper.writeValueAsString(content)));
          } catch (JsonProcessingException e) {
            log.error(CONTENT_LOG_PREFIX + "Unable to serialize content object: {} for indexing.",
                content.getId(), e);
            registerContentProblem(content, "Search Index Error: " + content.getId()
                + content.getCanonicalSourceFile() + " Exception: " + e, indexProblemCache);
            return Stream.empty();
          }
        })
        .toList();

    try {
      es.indexObject(sha, ContentIndextype.METADATA.toString(),
          objectMapper.writeValueAsString(Map.of("version", sha, "created", Instant.now().toString())), "general");
      es.indexObject(sha, ContentIndextype.METADATA.toString(),
          objectMapper.writeValueAsString(Map.of("tags", tagsList)), "tags");
      log.info(CONTENT_LOG_PREFIX + "Indexed metadata with {} tags", tagsList.size());

      long startTime = System.nanoTime();
      es.bulkIndex(sha, ContentIndextype.UNIT.toString(), serializeUnits(allUnits, objectMapper));
      es.bulkIndex(sha, ContentIndextype.PUBLISHED_UNIT.toString(), serializeUnits(publishedUnits, objectMapper));
      long endTime = System.nanoTime();
      log.info(CONTENT_LOG_PREFIX
          + "Bulk unit indexing took: {}ms", (endTime - startTime) / NANOSECONDS_IN_A_MILLISECOND);

      startTime = System.nanoTime();
      es.bulkIndex(sha, ContentIndextype.CONTENT_ERROR.toString(),
          serializeContentErrors(indexProblemCache, objectMapper));
      endTime = System.nanoTime();
      log.info(CONTENT_LOG_PREFIX
          + "Bulk content error indexing took: {}ms", (endTime - startTime) / NANOSECONDS_IN_A_MILLISECOND);
    } catch (JsonProcessingException e) {
      log.error(CONTENT_LOG_PREFIX + "Unable to serialise sha or tags");
    } catch (SegueSearchException e) {
      log.error(CONTENT_LOG_PREFIX + "Unable to index sha, tags, units or content errors.");
    }


    try {
      long startTime = System.nanoTime();
      es.bulkIndexWithIds(sha, ContentIndextype.CONTENT.toString(), contentToIndex);
      long endTime = System.nanoTime();
      log.info(CONTENT_LOG_PREFIX + "Bulk content indexing completed: {} items in {}ms",
          contentToIndex.size(),
          (endTime - startTime) / NANOSECONDS_IN_A_MILLISECOND);
    } catch (SegueSearchException e) {
      log.error(CONTENT_LOG_PREFIX + "Error during bulk index operation.", e);
    } catch (ActionRequestValidationException e) {
      log.error(CONTENT_LOG_PREFIX + "Error validating content during index", e);
    }
  }


  /**
   * This method will attempt to traverse the cache to ensure that all content references are valid.
   *
   * @param sha               version to validate integrity of.
   * @param gitCache          Data structure containing all content for a given sha.
   * @param indexProblemCache a map of problems found in the indexed content
   */
  @SuppressWarnings("checkstyle:OneStatementPerLine")
  private void recordContentErrors(final String sha, final Map<String, Content> gitCache,
                                   final Map<Content, List<String>> indexProblemCache) {

    Set<Content> allObjectsSeen = gitCache.values().stream()
        .flatMap(c -> flattenContentObjects(c).stream())
        .collect(Collectors.toSet());

    Map<String, Content> contentById = buildContentIndex(allObjectsSeen);
    ContentReferenceMap refMap = buildReferenceMap(sha, allObjectsSeen, indexProblemCache);

    recordMissingContentProblems(refMap.expectedIds(), contentById, refMap.incomingReferences(), indexProblemCache);
    recordPublishedToUnpublishedReferenceProblems(refMap.incomingReferences(), contentById, indexProblemCache);

    log.info("Validation processing ({}) complete. There are {} files with content problems",
        sanitiseInternalLogValue(sha), indexProblemCache.size());

    if (indexProblemCache.isEmpty()) {
      // Register a no-op style error to simplify application logic by ensuring there is always a content errors index
      Content dummyContentRecord = new Content();
      // "\uD83D\uDE0E"
      dummyContentRecord.setCanonicalSourceFile("😎");

      registerContentProblem(dummyContentRecord, "No content errors!", indexProblemCache);
    }
  }

  /**
   * Remove any content type indices related to a version.
   * If indices for only some of the content types at this version exist, they will be expunged. Trying to expunge an
   * index which does not exist for any reason will log an error but otherwise fail safely.
   *
   * @param version the commit sha of the content that we are interested in.
   */
  private void expungeAnyContentTypeIndicesRelatedToVersion(final String version) {
    log.info("Deleting existing indexes for version {}", sanitiseInternalLogValue(version));
    Arrays.stream(ContentIndextype.values())
        .forEach(contentIndexType -> es.expungeIndexFromSearchCache(version, contentIndexType.toString()));
  }

  /**
   * A successful indexing of a version means the creation of an index for each of the content types defined in
   * ContentIndextype. This method checks that they all exist for a particular version.
   *
   * @param version the content sha version to check.
   * @return True if indices exist for all expected content types at the provided version, else return false.
   */
  private boolean allContentTypesAreIndexedForVersion(final String version) {
    return Arrays.stream(ContentIndextype.values())
        .allMatch(contentIndexType -> es.hasIndex(version, contentIndexType.toString()));
  }

  /**
   * This method checks whether any indices have been created for this version.
   *
   * @param version the content sha version to check.
   * @return True if indices exist for any of the expected content types at the provided version, else return false.
   */
  private boolean anyContentTypesAreIndexedForVersion(final String version) {
    return Arrays.stream(ContentIndextype.values())
        .anyMatch(contentIndexType -> es.hasIndex(version, contentIndexType.toString()));
  }

  private String collateExpandableChildren(final Content content) {
    return flattenContentObjects(content).stream()
        .filter(child -> child != content && null != child.getExpandable()
            && Boolean.TRUE.equals(child.getExpandable()))
        .map(child -> null != child.getType() ? child.getType() : "undefined")
        .collect(Collectors.joining(","));
  }

  /**
   * This method will record content type specific errors for a single item of content.
   *
   * @param sha               version to validate integrity of.
   * @param content           a single item of content
   * @param indexProblemCache a map of problems found in the indexed content
   */
  private void recordContentTypeSpecificError(final String sha, final Content content,
                                              final Map<Content, List<String>> indexProblemCache) {
    registerContentProblemValueWithChildren(content, indexProblemCache);
    registerContentProblemNestedExpandables(content, indexProblemCache);
    registerContentProblemUnsupportedTypeExpandable(content, indexProblemCache);
    registerContentProblemsMediaInvalidProperties(sha, content, indexProblemCache);
    registerContentProblemQuestionMissingId(content, indexProblemCache);
    registerContentProblemsChoiceQuestionMissingChoicesOrAnswer(content, indexProblemCache);
    registerContentProblemEmailTemplateMissingPainTextContentField(content, indexProblemCache);
    registerContentProblemEventMissingOrInvalidEndDate(content, indexProblemCache);
    registerContentProblemsNumericQuestionInvalidChoicesOrUnits(content, indexProblemCache);

    if (content instanceof IsaacSymbolicQuestion && content.getClass().equals(IsaacSymbolicQuestion.class)) {
      registerContentProblemsSymbolicQuestionInvalidProperties(content, indexProblemCache);
    }

    if (content instanceof IsaacClozeQuestion q) {
      validateClozeQuestionChoiceItems(q, content, indexProblemCache);
    }
  }

  private void validateClozeQuestionChoiceItems(final IsaacClozeQuestion q, final Content content,
                                                final Map<Content, List<String>> indexProblemCache) {
    if (q.getChoices() == null) {
      return;
    }

    Integer[] expectedItemCount = {null};

    q.getChoices().stream()
        .filter(ItemChoice.class::isInstance)
        .map(ItemChoice.class::cast)
        .forEach(c -> {
          List<Item> items = c.getItems();
          if (items == null || items.isEmpty()) {
            registerContentProblem(content,
                "Cloze Question: " + q.getId() + " has choice with missing items!", indexProblemCache);
          } else {
            int itemCount = items.size();
            if (expectedItemCount[0] == null) {
              expectedItemCount[0] = itemCount;
            } else if (itemCount != expectedItemCount[0]) {
              registerContentProblem(content,
                  buildClozeQuestionIncorrectItemCountMessage(q, expectedItemCount[0], itemCount), indexProblemCache);
            }
          }
        });
  }

  private String buildClozeQuestionIncorrectItemCountMessage(final IsaacClozeQuestion q, final int expected,
                                                             final int actual) {
    return "Cloze Question: " + q.getId() + " has choice with incorrect number of items!"
        + " (Expected " + expected + ", got " + actual + "!)";
  }

  private void registerContentProblemsSymbolicQuestionInvalidProperties(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    IsaacSymbolicQuestion q = (IsaacSymbolicQuestion) content;
    if (q.getAvailableSymbols() != null) {
      q.getAvailableSymbols().stream()
          .filter(sym -> sym.contains("\\"))
          .forEach(sym -> registerContentProblem(content, SYMBOLIC_QUESTION + q.getId() + " has availableSymbol ("
              + sym + ") which contains a '\\' character.", indexProblemCache));
    }
    if (q.getChoices() != null) {
      q.getChoices().forEach(choice -> {
        if (choice instanceof Formula f) {
          String expr = f.getPythonExpression();
          if (expr != null && expr.contains("\\")) {
            registerContentProblem(content, SYMBOLIC_QUESTION + q.getId() + " has Formula ("
                + choice.getValue() + ") with pythonExpression which contains a '\\' character.", indexProblemCache);
          } else if (expr == null || expr.isEmpty()) {
            registerContentProblem(content, SYMBOLIC_QUESTION + q.getId() + " has Formula ("
                + choice.getValue() + ") with empty pythonExpression!", indexProblemCache);
          }
        } else {
          registerContentProblem(content, SYMBOLIC_QUESTION + q.getId() + " has non-Formula Choice ("
              + choice.getValue() + "). It must be deleted and a new Formula Choice created.", indexProblemCache);
        }
      });
    }
  }

  private void registerContentProblemsNumericQuestionInvalidChoicesOrUnits(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (!(content instanceof IsaacNumericQuestion q)) {
      return;
    }

    if (q.getChoices() != null) {
      q.getChoices().stream()
          .filter(Quantity.class::isInstance)
          .map(Quantity.class::cast)
          .forEach(quantity -> {
            try {
              new BigDecimal(quantity.getValue());
            } catch (NumberFormatException e) {
              registerContentProblem(content,
                  NUMERIC_QUESTION + q.getId() + " has Quantity (" + quantity.getValue()
                      + ")  with value that cannot be interpreted as a number. "
                      + "Users will never be able to match this answer.", indexProblemCache);
            }
            if (!q.getRequireUnits() && quantity.getUnits() != null && !quantity.getUnits().isEmpty()) {
              registerContentProblem(content, NUMERIC_QUESTION + q.getId()
                  + " has a Quantity with units but does not require units!", indexProblemCache);
            }
          });

      q.getChoices().stream()
          .filter(choice -> !(choice instanceof Quantity))
          .forEach(choice -> registerContentProblem(content, NUMERIC_QUESTION + q.getId() + " has non-Quantity Choice ("
              + choice.getValue() + "). It must be deleted and a new Quantity Choice created.", indexProblemCache));
    }

    if (q.getRequireUnits() && q.getDisplayUnit() != null && !q.getDisplayUnit().isEmpty()) {
      registerContentProblem(content,
          NUMERIC_QUESTION + q.getId() + " has a displayUnit set but also requiresUnits!"
              + " Units will be ignored for this question!", indexProblemCache);
    }
  }

  private void registerContentProblemEventMissingOrInvalidEndDate(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (!(content instanceof IsaacEventPage eventPage)) {
      return;
    }

    if (eventPage.getEndDate() == null) {
      registerContentProblem(content, "Event has no end date", indexProblemCache);
    } else if (eventPage.getDate() != null && eventPage.getEndDate().isBefore(eventPage.getDate())) {
      registerContentProblem(content, "Event has end date before start date", indexProblemCache);
    }
  }

  private void registerContentProblemEmailTemplateMissingPainTextContentField(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content instanceof EmailTemplate emailTemplate && (emailTemplate.getPlainTextContent() == null)) {
      this.registerContentProblem(content,
          "Email template should always have plain text content field", indexProblemCache);
    }
  }

  private void registerContentProblemsChoiceQuestionMissingChoicesOrAnswer(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (!(content instanceof ChoiceQuestion question && !"isaacQuestion".equals(content.getType()))) {
      return;
    }

    if (question.getChoices() == null || question.getChoices().isEmpty()) {
      registerContentProblemChoiceQuestionMissingChoices(indexProblemCache, question);
    } else {
      registerContentProblemChoiceQuestionMissingAnswer(indexProblemCache, question);
    }
  }

  private void registerContentProblemChoiceQuestionMissingAnswer(
      final Map<Content, List<String>> indexProblemCache, final ChoiceQuestion question) {
    if (question.getChoices().stream().noneMatch(Choice::isCorrect)) {
      registerContentProblem(question,
          QUESTION + question.getId() + " found without a correct answer. "
              + "This question will always be automatically marked as incorrect", indexProblemCache);
    }
  }

  private void registerContentProblemChoiceQuestionMissingChoices(
      final Map<Content, List<String>> indexProblemCache, final ChoiceQuestion question) {
    this.registerContentProblem(question,
        QUESTION + question.getId() + " found without any choice metadata. "
            + "This question will always be automatically " + "marked as incorrect", indexProblemCache);
  }

  private void registerContentProblemQuestionMissingId(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content instanceof Question && content.getId() == null) {
      this.registerContentProblem(content, QUESTION + content.getTitle() + " in " + content.getCanonicalSourceFile()
          + " found without a unique id. " + "This question cannot be logged correctly.", indexProblemCache);
    }
  }

  private void registerContentProblemsMediaInvalidProperties(
      final String sha, final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content instanceof Media media) {
      registerContentProblemMediaNotFoundOrTooLarge(sha, content, indexProblemCache, media);
      // check that there is some alt text.
      registerContentProblemMediaMissingAltText(content, indexProblemCache, media);
    }
  }

  private void registerContentProblemMediaMissingAltText(
      final Content content, final Map<Content, List<String>> indexProblemCache, final Media media) {
    if ((media.getAltText() == null || media.getAltText().isEmpty()) && !(media instanceof Video)
        && !media.getId().equals("eventThumbnail")) {
      this.registerContentProblem(content, "No altText attribute set for media element: " + media.getSrc()
          + " in Git source file " + content.getCanonicalSourceFile(), indexProblemCache);
    }
  }

  private void registerContentProblemMediaNotFoundOrTooLarge(
      final String sha, final Content content, final Map<Content, List<String>> indexProblemCache, final Media media) {
    if (media.getSrc() == null || media.getSrc().startsWith("http")) {
      return;
    }

    ByteArrayOutputStream fileData = null;
    try {
      fileData = database.getFileByCommitSha(sha, media.getSrc());
    } catch (IOException | UnsupportedOperationException e) {
      // File not found or operation not supported
    }

    if (fileData == null) {
      registerContentProblem(content, "Unable to find Image: " + media.getSrc()
          + " in Git. Could the reference be incorrect? SourceFile is " + content.getCanonicalSourceFile(),
          indexProblemCache);
    } else if (fileData.size() > MEDIA_FILE_SIZE_LIMIT) {
      int sizeInKiloBytes = fileData.size() / BYTES_IN_ONE_KILOBYTE;
      registerContentProblem(content, String.format("Image (%s) is %s kB and exceeds file size warning limit!",
          media.getSrc(), sizeInKiloBytes), indexProblemCache);
    }
  }

  private void registerContentProblemUnsupportedTypeExpandable(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (null != content.getExpandable() && content.getExpandable() && (null == content.getLayout()
        || !content.getLayout().equals("tabs")) && !(content instanceof CodeSnippet)) {
      this.registerContentProblem(content,
          "Content of type " + content.getType() + " in " + content.getCanonicalSourceFile() + " is "
              + "marked as expandable, but we do not support expanding this type of content yet. If this is a HTML"
              + " table, use class='expandable' in the table tag instead.", indexProblemCache);
    }
  }

  private void registerContentProblemNestedExpandables(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if ((null != content.getLayout() && content.getLayout().equals("tabs") || content instanceof CodeSnippet)
        && null != content.getChildren()) {
      String expandableChildrenLog = collateExpandableChildren(content);
      if (!expandableChildrenLog.isEmpty()) {
        this.registerContentProblem(content,
            "Content of type " + content.getType() + " in " + content.getCanonicalSourceFile() + " is "
                + "potentially expandable, but has expandable children of the following types: " + expandableChildrenLog
                + ". These children will have their expandable property disabled since we cannot handle nested "
                + "expandable content. Please make sure the parent content block is "
                + "marked as expandable instead, and that it's children blocks have the expandable property "
                + "disabled.", indexProblemCache);
      }
    }
  }

  private void registerContentProblemValueWithChildren(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content.getValue() != null && content.getChildren() != null && !content.getChildren().isEmpty()) {
      String firstLine = content.getId() != null ? "Content: " + content.getId() : "Content";

      this.registerContentProblem(content, firstLine + " in " + content.getCanonicalSourceFile()
          + " found with both children and a value. "
          + "Content objects are only allowed to have one or the other.", indexProblemCache);

      log.error(
          "Invalid content item detected: The object with ID ({}) has both children and a value.",
          content.getCanonicalSourceFile()
      );
    }
  }

  private Map<String, Content> buildContentIndex(final Set<Content> allObjectsSeen) {
    return allObjectsSeen.stream()
        .filter(c -> c.getId() != null)
        .collect(Collectors.toMap(Content::getId, c -> c));
  }

  private ContentReferenceMap buildReferenceMap(final String sha, final Set<Content> allObjectsSeen,
                                                final Map<Content, List<String>> indexProblemCache) {
    Set<String> expectedIds = new HashSet<>();
    Map<String, Set<Content>> incomingReferences = new HashMap<>();

    allObjectsSeen.forEach(c -> {
      if (c.getRelatedContent() != null) {
        expectedIds.addAll(c.getRelatedContent());
        c.getRelatedContent().forEach(id ->
            incomingReferences.computeIfAbsent(id, k -> new HashSet<>()).add(c));
      }

      try {
        recordContentTypeSpecificError(sha, c, indexProblemCache);
      } catch (NullPointerException e) {
        log.warn("Failed processing content errors in file: {}", c.getCanonicalSourceFile());
      }
    });

    return new ContentReferenceMap(expectedIds, incomingReferences);
  }

  private void recordMissingContentProblems(final Set<String> expectedIds, final Map<String, Content> contentById,
                                            final Map<String, Set<Content>> incomingReferences,
                                            final Map<Content, List<String>> indexProblemCache) {
    Set<String> missingContent = new HashSet<>(expectedIds);
    missingContent.removeAll(contentById.keySet());

    // Diagnose: is the ID present in the cache but as an augmented child ID?
    missingContent.forEach(id -> incomingReferences.get(id).forEach(src -> {
      List<String> augmentedMatches = contentById.keySet().stream()
          .filter(k -> k.endsWith(Constants.ID_SEPARATOR + id))
          .toList();
      String diagnosis = augmentedMatches.isEmpty() ? ""
          : " (Note: Found augmented forms in index: " + augmentedMatches
          + " — the reference may use a bare ID but the content was indexed as a child)";
      this.registerContentProblem(src, "The id '" + id + "' was referenced by "
          + src.getCanonicalSourceFile() + " but the content with that "
          + "ID cannot be found." + diagnosis, indexProblemCache);
    }));
    if (!missingContent.isEmpty()) {
      log.warn(CONTENT_LOG_PREFIX + "Referential integrity broken for ({}) related Content items. "
          + "The following ids are referenced but do not exist: {}", missingContent.size(), missingContent);
    }
  }

  private void recordPublishedToUnpublishedReferenceProblems(final Map<String, Set<Content>> incomingReferences,
                                                             final Map<String, Content> contentById,
                                                             final Map<Content, List<String>> indexProblemCache) {
    incomingReferences.forEach((refTargetId, referenceSources) -> {
      Content refTarget = contentById.get(refTargetId);
      if (refTarget != null && !refTarget.getPublished()) {
        referenceSources.stream()
            .filter(Content::getPublished)
            .forEach(src -> registerContentProblem(src,
                "Content is published, but references unpublished content '" + refTargetId + "'.",
                indexProblemCache));
      }
    });
  }

  private List<String> serializeUnits(final Map<String, String> units, final ObjectMapper objectMapper) {
    return units.entrySet().stream().map(entry -> {
      try {
        return objectMapper.writeValueAsString(Map.of("cleanKey", entry.getKey(), "unit", entry.getValue()));
      } catch (JsonProcessingException jsonProcessingException) {
        log.error("Unable to serialise unit entry for unit: {}", entry.getValue());
        return null;
      }
    }).filter(Objects::nonNull).toList();
  }

  private List<String> serializeContentErrors(final Map<Content, List<String>> indexProblemCache,
                                              final ObjectMapper objectMapper) {
    return indexProblemCache.entrySet().stream().map(e -> {
      try {
        return objectMapper.writeValueAsString(Map.of(
            "canonicalSourceFile", e.getKey().getCanonicalSourceFile(),
            "id", e.getKey().getId() == null ? "" : e.getKey().getId(),
            "title", e.getKey().getTitle() == null ? "" : e.getKey().getTitle(),
            "published", e.getKey().getPublished() == null ? "" : e.getKey().getPublished(),
            "errors", e.getValue().toArray()));
      } catch (JsonProcessingException jsonProcessingException) {
        log.error("Unable to serialise content error entry from file: {}", e.getKey().getCanonicalSourceFile());
        return null;
      }
    }).filter(Objects::nonNull).toList();
  }
}
