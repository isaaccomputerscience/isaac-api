package uk.ac.cam.cl.dtg.segue.etl;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuizSection;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.segue.database.GitDb;

/**
 * Loads content from Git repository and populates the in-memory cache.
 * - Walks Git tree for JSON files
 * - Deserializes content objects
 * - Applies content augmentation
 * - Manages caching and deduplication
 */
public class ContentGitLoader {

  private static final Logger log = LoggerFactory.getLogger(ContentGitLoader.class);
  private static final String CONTENT_LOG_PREFIX = "CONTENT - ";
  private static final String THE_FOLLOWING_ERROR_OCCURRED = ". The following error occurred: ";

  private final GitDb database;
  private final ContentMapperUtils mapperUtils;
  private final ContentAugmenter augmenter;

  public ContentGitLoader(final GitDb database,
                          final ContentMapperUtils mapperUtils,
                          final ContentAugmenter augmenter) {
    this.database = database;
    this.mapperUtils = mapperUtils;
    this.augmenter = augmenter;
  }

  /**
   * Builds the content index by walking the Git tree and loading JSON files.
   *
   * @param version SHA or ref to load content from
   * @param context indexing context containing cache and related collections
   */
  public synchronized void buildGitContentIndex(final String version, final IndexingContext context) {
    try (TreeWalk treeWalk = database.getTreeWalk(version, ".json")) {
      Repository repository = database.getGitRepository();
      while (treeWalk.next()) {
        processJsonFile(treeWalk, repository, context);
      }
    } catch (IOException e) {
      log.error("IOException while trying to access git repository. ", e);
      throw new ContentIndexingException("Unable to index content, due to an IOException.", e);
    }
  }

  /**
   * Processes a single JSON file from the Git tree.
   */
  private void processJsonFile(final TreeWalk treeWalk,
                               final Repository repository,
                               final IndexingContext context) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
      loader.copyTo(out);

      ObjectMapper objectMapper = mapperUtils.getSharedContentObjectMapper();
      parseAndIndexJsonContent(objectMapper, out.toString(), treeWalk.getPathString(), context);
    } catch (Exception e) {
      log.error(CONTENT_LOG_PREFIX + "Unexpected error while processing file {}: {}",
          treeWalk.getPathString(),
          e.getMessage(),
          e);
      Content dummyContent = new Content();
      dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
      context.registerProblem(dummyContent,
          "Index failure - Unexpected error while processing file - " + treeWalk.getPathString()
              + THE_FOLLOWING_ERROR_OCCURRED + e.getMessage());
    }
  }

  /**
   * Parses JSON and indexes the content object.
   */
  private void parseAndIndexJsonContent(final ObjectMapper objectMapper,
                                        final String jsonContent,
                                        final String filePath,
                                        final IndexingContext context) {
    try {
      Content content = (Content) objectMapper.readValue(jsonContent, ContentBase.class);

      if (context.shouldSkipUnpublished(content)) {
        log.info("Skipping unpublished content: {}", content.getId());
        return;
      }

      content = this.augmenter.augmentChildContent(content, filePath, null, content.getPublished());

      if (null != content) {
        log.info(CONTENT_LOG_PREFIX + "Processing file: {} (type: {}, id: {})", filePath,
            content.getType(), content.getId());
        indexContentObject(context, filePath, content);
      }
    } catch (JsonMappingException e) {
      log.warn(CONTENT_LOG_PREFIX + "Unable to parse the json file found {} as a content object. "
          + "Skipping file due to error: \n {}", filePath, e.getMessage());
      Content dummyContent = new Content();
      dummyContent.setCanonicalSourceFile(filePath);
      context.registerProblem(dummyContent, "Index failure - Unable to parse json file found - "
          + filePath + THE_FOLLOWING_ERROR_OCCURRED + e.getMessage());
    } catch (IOException e) {
      log.error("IOException while trying to parse {}", filePath, e);
      Content dummyContent = new Content();
      dummyContent.setCanonicalSourceFile(filePath);
      context.registerProblem(dummyContent,
          "Index failure - Unable to read the json file found - " + filePath
              + THE_FOLLOWING_ERROR_OCCURRED + e.getMessage());
    }
  }

  /**
   * Indexes a content object and its children into the cache.
   */
  private void indexContentObject(final IndexingContext context, final String treeWalkPath, final Content content) {
    for (Content flattenedContent : augmenter.flattenContentObjects(content)) {
      validateAndCacheContent(flattenedContent, content, treeWalkPath, context);
    }
  }

  /**
   * Validates and caches a content object, handling duplicates and ID collisions.
   */
  private void validateAndCacheContent(final Content flattenedContent,
                                       final Content parentContent,
                                       final String treeWalkPath,
                                       final IndexingContext context) {
    if (flattenedContent.getId() == null) {
      return;
    }

    // Hard structural/id validations gate what enters the index: invalid content is reported AND
    // excluded from the cache (and therefore from Elasticsearch), as in the original indexer.
    if (flattenedContent instanceof IsaacQuiz) {
      List<ContentBase> children = flattenedContent.getChildren();
      if (children != null && children.stream().anyMatch(c -> !(c instanceof IsaacQuizSection))) {
        log.info(CONTENT_LOG_PREFIX + "IsaacQuiz ({}) contains top-level non-quiz sections. Skipping.",
            flattenedContent.getId());
        context.registerProblem(flattenedContent, "Index failure - Invalid content type among quiz sections. "
            + "Quizzes can only contain quiz sections in the top-level children array.");
        return;
      }
    }

    if (flattenedContent.getId().length() > Constants.MAXIMUM_CONTENT_ID_LENGTH) {
      log.info(CONTENT_LOG_PREFIX + "Content ID too long: {}", flattenedContent.getId());
      context.registerProblem(flattenedContent, "Content ID too long: " + flattenedContent.getId());
      return;
    }

    if (flattenedContent.getId().contains(".")) {
      log.info(CONTENT_LOG_PREFIX + "Resource with invalid ID ({}) detected. Skipping {}",
          flattenedContent.getId(), treeWalkPath);
      context.registerProblem(flattenedContent, "Index failure - Invalid ID " + flattenedContent.getId()
          + " found in file " + treeWalkPath + ". Must not contain restricted characters.");
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

    log.info("Resource with duplicate ID ({}) detected in cache. Skipping {}", flattenedContent.getId(), treeWalkPath);
    context.registerProblem(flattenedContent, String.format(
        "Index failure - Duplicate ID (%s) found in files (%s) and (%s): only one will be available.",
        flattenedContent.getId(),
        treeWalkPath,
        context.contentCache().get(flattenedContent.getId()).getCanonicalSourceFile()));
  }

  /**
   * Registers tags from content.
   */
  private synchronized void registerTags(final Set<String> tags, final Set<String> tagsList) {
    if (tags != null) {
      tags.stream().map(String::trim).forEach(tagsList::add);
    }
  }

  /**
   * Registers units from numeric questions.
   */
  private synchronized void registerUnits(final IsaacNumericQuestion question,
                                          final Map<String, String> allUnits,
                                          final Map<String, String> publishedUnits) {
    if (question.getChoices() != null) {
      for (Object choice : question.getChoices()) {
        if (choice instanceof Quantity quantity
            && quantity.getUnits() != null
            && !quantity.getUnits().isEmpty()) {
          allUnits.put(quantity.getUnits(), quantity.getUnits());
          if (question.getPublished() != null && question.getPublished()) {
            publishedUnits.put(quantity.getUnits(), quantity.getUnits());
          }
        }
      }
    }
  }

}