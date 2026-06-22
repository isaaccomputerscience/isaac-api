package uk.ac.cam.cl.dtg.segue.etl;

import static uk.ac.cam.cl.dtg.segue.api.Constants.ContentIndextype;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseInternalLogValue;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.etl.validators.ContentValidator;

/**
 * Orchestrates the content indexing pipeline.
 * Delegates to specialized classes for Git loading, validation, Elasticsearch submission, and reporting.
 */
public class ContentIndexer {
  private static final Logger log = LoggerFactory.getLogger(ContentIndexer.class);

  private static final ConcurrentHashMap<String, Boolean> VERSION_LOCKS = new ConcurrentHashMap<>();
  private static final int NANOSECONDS_IN_A_MILLISECOND = 1000000;
  private static final String CONTENT_LOG_PREFIX = "CONTENT - ";

  private final ElasticSearchIndexer es;
  private final GitDb database;
  private final ContentValidator validator;
  private final ContentGitLoader gitLoader;
  private final ContentElasticSearchSubmitter submitter;
  private final IndexingReportGenerator reporter;

  @Inject
  public ContentIndexer(final GitDb database, final ElasticSearchIndexer es, final ContentMapperUtils mapperUtils) {
    this.database = database;
    this.es = es;
    ContentAugmenter augmenter = new ContentAugmenter();
    this.validator = new ContentValidator(database, augmenter);
    this.gitLoader = new ContentGitLoader(database, mapperUtils, augmenter);
    this.submitter = new ContentElasticSearchSubmitter(es, mapperUtils);
    this.reporter = new IndexingReportGenerator();
  }

  /**
   * Orchestrates the indexing pipeline: load from Git, validate, submit to Elasticsearch, and report.
   */
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

      if (allContentTypesAreIndexedForVersion(version)) {
        log.info(CONTENT_LOG_PREFIX + "Content already indexed: {}", sanitiseInternalLogValue(version));
        return;
      }

      log.info(CONTENT_LOG_PREFIX + "Rebuilding content index for sha: {}",
          sanitiseInternalLogValue(version));

      long totalStartTime = System.nanoTime();

      // Create context and load content from Git
      IndexingContext context = IndexingContext.forVersion(true);
      try {
        gitLoader.buildGitContentIndex(version, context);
        long loadTime = (System.nanoTime() - totalStartTime) / NANOSECONDS_IN_A_MILLISECOND;
        log.info(CONTENT_LOG_PREFIX + "Finished populating Git content cache, took: {}ms", loadTime);

        // Validate content
        log.info(CONTENT_LOG_PREFIX + "Beginning to record content errors");
        long validateStart = System.nanoTime();
        validator.recordContentErrors(version, context);
        long validateTime = (System.nanoTime() - validateStart) / NANOSECONDS_IN_A_MILLISECOND;
        log.info(CONTENT_LOG_PREFIX + "Finished recording content errors, took: {}ms", validateTime);

        // Submit to Elasticsearch
        long indexStart = System.nanoTime();
        submitter.buildElasticSearchIndex(version, context);
        long indexTime = (System.nanoTime() - indexStart) / NANOSECONDS_IN_A_MILLISECOND;
        log.info(CONTENT_LOG_PREFIX + "Finished building ElasticSearch index, took: {}ms", indexTime);

        // Verify
        if (!allContentTypesAreIndexedForVersion(version)) {
          expungeAnyContentTypeIndicesRelatedToVersion(version);
          throw new Exception(String.format("Failed to index version %s. Don't know why.", version));
        }

        long totalTime = (System.nanoTime() - totalStartTime) / NANOSECONDS_IN_A_MILLISECOND;
        log.info(CONTENT_LOG_PREFIX + "Finished indexing version {}, total time: {}ms",
            sanitiseInternalLogValue(version), totalTime);
      } finally {
        // Generate and log the indexing report whether indexing succeeded or failed, so that any
        // validation problems collected before a failure are still reported.
        reporter.generateIndexingReport(version, context);
      }

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
   * Remove any content type indices related to a version.
   * If indices for only some of the content types at this version exist, they will be expunged.
   *
   * @param version the commit sha of the content that we are interested in.
   */
  private void expungeAnyContentTypeIndicesRelatedToVersion(final String version) {
    log.info("Deleting existing indexes for version {}", sanitiseInternalLogValue(version));
    Arrays.stream(ContentIndextype.values())
        .forEach(contentIndexType -> es.expungeIndexFromSearchCache(version, contentIndexType.toString()));
  }

  /**
   * Checks that all expected content type indices exist for a version.
   *
   * @param version the content sha version to check.
   * @return True if all content type indices exist for this version.
   */
  private boolean allContentTypesAreIndexedForVersion(final String version) {
    return Arrays.stream(ContentIndextype.values())
        .allMatch(contentIndexType -> es.hasIndex(version, contentIndexType.toString()));
  }
}