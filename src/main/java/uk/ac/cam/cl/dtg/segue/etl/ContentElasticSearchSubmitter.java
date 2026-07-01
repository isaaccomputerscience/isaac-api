package uk.ac.cam.cl.dtg.segue.etl;

import static com.google.common.collect.Maps.immutableEntry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.ActionRequestValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

/**
 * Submits indexed content to Elasticsearch.
 */
public class ContentElasticSearchSubmitter {

  private static final Logger log = LoggerFactory.getLogger(ContentElasticSearchSubmitter.class);
  private static final String CONTENT_LOG_PREFIX = "CONTENT - ";

  private final ElasticSearchIndexer es;
  private final ContentMapperUtils mapperUtils;

  public ContentElasticSearchSubmitter(final ElasticSearchIndexer es, final ContentMapperUtils mapperUtils) {
    this.es = es;
    this.mapperUtils = mapperUtils;
  }

  /**
   * Builds and submits the Elasticsearch index:
   * 1. Expunges any existing partial indexes for this version
   * 2. Submits units, published units, and content errors
   * 3. Submits content objects
   */
  public synchronized void buildElasticSearchIndex(final String sha, final IndexingContext context) {
    expungeExistingIndexes(sha);

    ObjectMapper objectMapper = mapperUtils.getSharedContentObjectMapper();
    submitMetadata(sha, context, objectMapper);
    submitUnitsAndErrors(sha, context, objectMapper);
    submitContent(sha, context, objectMapper);
  }

  /**
   * Submits the METADATA index documents (version SHA + collated tags).
   *
   * <p>This index is mandatory: the orchestrator's success check requires an index for every
   * {@link Constants.ContentIndextype}, and {@code GitContentManager#getCurrentContentSHA()} and
   * {@code getTagsList()} read the {@code general} and {@code tags} documents respectively.
   */
  private void submitMetadata(final String sha, final IndexingContext context, final ObjectMapper objectMapper) {
    try {
      es.indexObject(sha, Constants.ContentIndextype.METADATA.toString(),
          objectMapper.writeValueAsString(Map.of("version", sha, "created", Instant.now().toString())), "general");
      es.indexObject(sha, Constants.ContentIndextype.METADATA.toString(),
          objectMapper.writeValueAsString(Map.of("tags", context.tagsList())), "tags");
      log.info(CONTENT_LOG_PREFIX + "Indexed metadata with {} tags", context.tagsList().size());
    } catch (JsonProcessingException e) {
      log.error(CONTENT_LOG_PREFIX + "Unable to serialise version metadata or tags.", e);
    } catch (SegueSearchException e) {
      log.error(CONTENT_LOG_PREFIX + "Unable to index version metadata or tags.", e);
    }
  }

  /**
   * Expunges any existing partial indexes for the given version.
   */
  private void expungeExistingIndexes(final String sha) {
    if (anyContentTypesAreIndexedForVersion(sha)) {
      Arrays.stream(Constants.ContentIndextype.values())
          .forEach(type -> es.expungeIndexFromSearchCache(sha, type.toString()));
    }
  }

  /**
   * Submits units and content error metadata to Elasticsearch.
   */
  private void submitUnitsAndErrors(final String sha, final IndexingContext context, final ObjectMapper objectMapper) {
    try {
      List<String> serializedUnits = serializeUnits(context.allUnits(), objectMapper);
      List<String> serializedPublishedUnits = serializeUnits(context.publishedUnits(), objectMapper);
      List<String> serializedErrors = serializeContentErrors(context.indexProblemCache(), objectMapper);

      es.bulkIndex(sha, Constants.ContentIndextype.UNIT.toString(), serializedUnits);
      es.bulkIndex(sha, Constants.ContentIndextype.PUBLISHED_UNIT.toString(), serializedPublishedUnits);
      es.bulkIndex(sha, Constants.ContentIndextype.CONTENT_ERROR.toString(), serializedErrors);
      log.info(CONTENT_LOG_PREFIX + "Bulk unit and error indexing completed");
    } catch (SegueSearchException e) {
      log.error(CONTENT_LOG_PREFIX + "Unable to index units or content errors.", e);
    }
  }

  /**
   * Submits content objects to Elasticsearch.
   */
  private void submitContent(final String sha, final IndexingContext context, final ObjectMapper objectMapper) {
    try {
      List<Map.Entry<String, String>> contentToIndex = serializeContent(context.contentCache().values(), objectMapper);
      // Note: an empty list is fine here — bulkIndexWithIds creates the (empty) CONTENT index so the
      // post-index verification still passes, rather than skipping it.
      es.bulkIndexWithIds(sha, Constants.ContentIndextype.CONTENT.toString(), contentToIndex);
      log.info(CONTENT_LOG_PREFIX + "Bulk content indexing completed: {} items", contentToIndex.size());
    } catch (SegueSearchException e) {
      log.error(CONTENT_LOG_PREFIX + "Error during bulk content index operation.", e);
    } catch (ActionRequestValidationException e) {
      log.error(CONTENT_LOG_PREFIX + "Error validating content during index", e);
    }
  }

  /**
   * Serializes content objects to id-json pairs.
   */
  private List<Map.Entry<String, String>> serializeContent(final Iterable<Content> contents,
                                                           final ObjectMapper objectMapper) {
    try {
      List<Map.Entry<String, String>> result = new ArrayList<>();
      contents.forEach(content -> {
        try {
          result.add(immutableEntry(content.getId(), objectMapper.writeValueAsString(content)));
        } catch (JsonProcessingException e) {
          log.error(CONTENT_LOG_PREFIX + "Unable to serialise content for indexing", e);
        }
      });
      return result;
    } catch (Exception e) {
      log.error(CONTENT_LOG_PREFIX + "Error during content serialization", e);
      return new ArrayList<>();
    }
  }

  /**
   * Checks if any content type is already indexed for this version.
   */
  private boolean anyContentTypesAreIndexedForVersion(final String version) {
    return Arrays.stream(Constants.ContentIndextype.values()).anyMatch(type -> es.hasIndex(version, type.toString()));
  }

  /**
   * Serializes units map to a list of JSON strings.
   */
  private List<String> serializeUnits(final Map<String, String> units, final ObjectMapper objectMapper) {
    List<String> result = new ArrayList<>();
    try {
      units.forEach((key, value) -> {
        try {
          result.add(objectMapper.writeValueAsString(
              Map.of("cleanKey", key, "unit", value)));
        } catch (Exception e) {
          log.warn("Error serializing unit {}: {}", key, e.getMessage());
        }
      });
    } catch (Exception e) {
      log.warn("Error during units serialization: {}", e.getMessage());
    }
    return result;
  }

  /**
   * Serializes content errors to a list of JSON strings.
   */
  private List<String> serializeContentErrors(final Map<Content, List<String>> indexProblemCache,
                                              final ObjectMapper objectMapper) {
    List<String> result = new ArrayList<>();
    try {
      indexProblemCache.forEach((content, errors) -> {
        try {
          // Field set must match GitContentManager#getProblemMap(), which reads
          // canonicalSourceFile, id, title, published and errors to render the admin problem report.
          String json = objectMapper.writeValueAsString(
              java.util.Map.of(
                  "canonicalSourceFile",
                  content.getCanonicalSourceFile() != null ? content.getCanonicalSourceFile() : "",
                  "id", content.getId() != null ? content.getId() : "",
                  "title", content.getTitle() != null ? content.getTitle() : "",
                  "published", content.getPublished() != null ? content.getPublished() : "",
                  "errors", errors
              ));
          result.add(json);
        } catch (Exception e) {
          log.warn("Error serializing content error for {}: {}", content.getId(), e.getMessage());
        }
      });
    } catch (Exception e) {
      log.warn("Error during content errors serialization: {}", e.getMessage());
    }
    return result;
  }
}