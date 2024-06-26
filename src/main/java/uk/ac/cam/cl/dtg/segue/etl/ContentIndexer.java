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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.elasticsearch.action.ActionRequestValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCard;
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

  private final ElasticSearchIndexer es;
  private final GitDb database;
  private final ContentMapperUtils mapperUtils;

  private static final int MEDIA_FILE_SIZE_LIMIT = 300 * 1024; // Bytes
  private static final int NANOSECONDS_IN_A_MILLISECOND = 1000000;

  @Inject
  public ContentIndexer(final GitDb database, final ElasticSearchIndexer es, final ContentMapperUtils mapperUtils) {
    this.database = database;
    this.es = es;
    this.mapperUtils = mapperUtils;
  }


  void loadAndIndexContent(final String version) throws Exception, VersionLockedException {

    // Take version lock or fail
    Boolean alreadyLocked = VERSION_LOCKS.putIfAbsent(version, true);

    if (Boolean.TRUE.equals(alreadyLocked)) {
      throw new VersionLockedException(version);
    }

    log.info("Acquired lock for version " + sanitiseInternalLogValue(version) + ". Indexing.");

    try {

      database.fetchLatestFromRemote();

      // Now we have acquired the lock check in case someone else has already indexed this version.
      // The case where only some of the content types have been successfully indexed for this version, should
      // never happen but is covered by an expunge at the start of #buildElasticSearchIndex(...).
      if (allContentTypesAreIndexedForVersion(version)) {
        log.info("Content already indexed: " + sanitiseInternalLogValue(version));
        return;
      }

      log.info(String.format(
          "Rebuilding content index as sha (%s) does not exist in search provider.",
          sanitiseInternalLogValue(version)));

      Map<String, Content> contentCache = new HashMap<>();
      Set<String> tagsList = new HashSet<>();
      Map<String, String> allUnits = new HashMap<>();
      Map<String, String> publishedUnits = new HashMap<>();
      Map<Content, List<String>> indexProblemCache = new HashMap<>();

      long totalStartTime;
      long startTime;
      long endTime;

      totalStartTime = System.nanoTime();
      buildGitContentIndex(version, true, contentCache, tagsList, allUnits, publishedUnits, indexProblemCache);
      endTime = System.nanoTime();

      log.info(
          "Finished populating Git content cache, took: {}ms",
          (endTime - totalStartTime) / NANOSECONDS_IN_A_MILLISECOND
      );
      log.info("Beginning to record content errors");

      startTime = System.nanoTime();
      recordContentErrors(version, contentCache, indexProblemCache);
      endTime = System.nanoTime();

      log.info("Finished recording content errors, took: {}ms", (endTime - startTime) / NANOSECONDS_IN_A_MILLISECOND);

      startTime = System.nanoTime();
      buildElasticSearchIndex(version, contentCache, tagsList, allUnits, publishedUnits, indexProblemCache);
      endTime = System.nanoTime();
      log.info("Finished indexing git content cache, took: {}ms", (endTime - startTime) / NANOSECONDS_IN_A_MILLISECOND);

      // Verify the version requested is now available
      if (!allContentTypesAreIndexedForVersion(version)) {
        expungeAnyContentTypeIndicesRelatedToVersion(version);
        throw new Exception(String.format("Failed to index version %s. Don't know why.", version));
      }

      log.info("Finished indexing version " + sanitiseInternalLogValue(version) + ", took: "
          + ((endTime - totalStartTime) / NANOSECONDS_IN_A_MILLISECOND) + "ms");

    } finally {
      VERSION_LOCKS.remove(version);
    }

  }

  void setNamedVersion(final String alias, final String version) {
    List<String> allContentTypes = Arrays.stream(ContentIndextype.values())
        .map(ContentIndextype::toString).collect(Collectors.toList());
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
   * @throws ContentManagerException  if the SHA is null or the associated resource cannot be accessed
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
      log.info("Populating git content cache based on sha " + sanitiseInternalLogValue(sha) + " ...");

      // Traverse the git repository looking for the .json files
      while (treeWalk.next()) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
        loader.copyTo(out);

        // setup object mapper to use preconfigured deserializer
        // module. Required to deal with type polymorphism
        ObjectMapper objectMapper = mapperUtils.getSharedContentObjectMapper();

        Content content;
        try {
          content = (Content) objectMapper.readValue(out.toString(), ContentBase.class);

          // check if we only want to index published content
          if (!includeUnpublished && !content.getPublished()) {
            log.debug("Skipping unpublished content: {}", content.getId());
            continue;
          }

          content = this.augmentChildContent(content, treeWalk.getPathString(), null, content.getPublished());

          if (null != content) {
            indexContentObject(contentCache, tagsList, allUnits, publishedUnits, indexProblemCache,
                treeWalk.getPathString(), content);
          }
        } catch (JsonMappingException e) {
          log.debug(String.format("Unable to parse the json file found %s as a content object. "
              + "Skipping file due to error: \n %s", treeWalk.getPathString(), e.getMessage()));
          Content dummyContent = new Content();
          dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
          this.registerContentProblem(dummyContent, "Index failure - Unable to parse json file found - "
              + treeWalk.getPathString() + ". The following error occurred: " + e.getMessage(), indexProblemCache);
        } catch (IOException e) {
          log.error("IOException while trying to parse {}", treeWalk.getPathString(), e);
          Content dummyContent = new Content();
          dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
          this.registerContentProblem(dummyContent,
              "Index failure - Unable to read the json file found - " + treeWalk.getPathString()
                  + ". The following error occurred: " + e.getMessage(), indexProblemCache);
        }
      }

      repository.close();
      log.debug("Tags available {}", tagsList);
      log.debug("All units: {}", allUnits);
      log.info("Git content cache population for " + sanitiseInternalLogValue(sha) + " completed!");

    } catch (IOException e) {
      log.error("IOException while trying to access git repository. ", e);
      throw new ContentManagerException("Unable to index content, due to an IOException.");
    }
  }

  private void indexContentObject(
      final Map<String, Content> contentCache, final Set<String> tagsList, final Map<String, String> allUnits,
      final Map<String, String> publishedUnits, final Map<Content, List<String>> indexProblemCache,
      final String treeWalkPath,
      final Content content) {
    // Walk the content for site-wide searchable fields
    StringBuilder searchableContentBuilder = new StringBuilder();
    this.collateSearchableContent(content, searchableContentBuilder);
    content.setSearchableContent(searchableContentBuilder.toString());

    // add children (and parent) from flattened Set to
    // cache if they have ids
    for (Content flattenedContent : this.flattenContentObjects(content)) {
      if (flattenedContent.getId() == null) {
        continue;
      }

      // Prevents ETL indexing of quizzes that contain anything that is not an IsaacQuizSection
      // in the top-level children array.
      // NOTE: I'm not sure if this is the right place for this but I couldn't find a better one.
      // This also seems to be the only time we can prevent a file from being indexed entirely.
      if (flattenedContent instanceof IsaacQuiz) {
        List<ContentBase> children = flattenedContent.getChildren();
        if (children.stream().anyMatch(c -> !(c instanceof IsaacQuizSection))) {
          log.debug("IsaacQuiz ({}) contains top-level non-quiz sections. Skipping.", flattenedContent.getId());
          this.registerContentProblem(flattenedContent, "Index failure - Invalid "
              + "content type among quiz sections. Quizzes can only contain quiz sections "
              + "in the top-level children array.", indexProblemCache);
          continue;
        }
      }

      if (flattenedContent.getId().length() > MAXIMUM_CONTENT_ID_LENGTH) {
        log.debug("Content ID too long: {}", flattenedContent.getId());
        this.registerContentProblem(flattenedContent, "Content ID too long: " + flattenedContent.getId(),
            indexProblemCache);
        continue;
      }

      if (flattenedContent.getId().contains(".")) {
        // Otherwise, duplicate IDs with different content,
        // therefore log an error
        log.debug("Resource with invalid ID ({}) detected in cache. Skipping {}", content.getId(), treeWalkPath);

        this.registerContentProblem(flattenedContent, "Index failure - Invalid ID "
            + flattenedContent.getId() + " found in file " + treeWalkPath
            + ". Must not contain restricted characters.", indexProblemCache);
        continue;
      }

      // check if we have seen this key before if
      // we have then we don't want to add it
      // again
      if (!contentCache.containsKey(flattenedContent.getId())) {
        // It must be new so we can add it
        log.debug("Loading into cache: {} ({}) from {}", flattenedContent.getId(), flattenedContent.getType(),
            treeWalkPath);
        contentCache.put(flattenedContent.getId(), flattenedContent);
        registerTags(flattenedContent.getTags(), tagsList);

        // If this is a numeric question, extract any
        // units from its answers.

        if (flattenedContent instanceof IsaacNumericQuestion) {
          registerUnits((IsaacNumericQuestion) flattenedContent, allUnits, publishedUnits);
        }

        continue; // our work here is done
      }

      // shaCache contains key already, compare the
      // content
      if (contentCache.get(flattenedContent.getId()).equals(flattenedContent)) {
        // content is the same therefore it is just
        // reuse of a content object so that is
        // fine.
        log.debug("Resource ({}) already seen in cache. Skipping {}", content.getId(), treeWalkPath);
        continue;
      }

      // Otherwise, duplicate IDs with different content,
      // therefore log an error
      log.debug("Resource with duplicate ID ({}) detected in cache. Skipping {}", content.getId(), treeWalkPath);
      this.registerContentProblem(flattenedContent, String.format(
              "Index failure - Duplicate ID (%s) found in files (%s) and (%s): only one will be available.",
              content.getId(), treeWalkPath, contentCache.get(flattenedContent.getId()).getCanonicalSourceFile()),
          indexProblemCache);
    }
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
      log.debug("Found question without id {} {}", content.getTitle(), canonicalSourceFile);
    }

    // Try to figure out the parent ids.
    String newParentId;
    if (null == parentId && content.getId() != null) {
      newParentId = content.getId();
    } else {
      if (content.getId() != null) {
        newParentId = parentId + Constants.ID_SEPARATOR + content.getId();
      } else {
        newParentId = parentId;
      }
    }

    content.setCanonicalSourceFile(canonicalSourceFile);

    if (!content.getChildren().isEmpty()) {
      for (ContentBase cb : content.getChildren()) {
        if (cb instanceof Content) {
          Content c = (Content) cb;

          this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
        }
      }
    }

    if (content instanceof Choice) {
      Choice choice = (Choice) content;
      this.augmentChildContent((Content) choice.getExplanation(), canonicalSourceFile,
          newParentId, parentPublished);
    }

    // hack to get cards to count as children:
    if (content instanceof IsaacCardDeck) {
      for (IsaacCard card : ((IsaacCardDeck) content).getCards()) {
        this.augmentChildContent(card, canonicalSourceFile, newParentId, parentPublished);
      }
    }

    if (content instanceof Question) {
      Question question = (Question) content;
      if (question.getHints() != null) {
        for (ContentBase cb : question.getHints()) {
          Content c = (Content) cb;
          this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
        }
      }

      // Augment question answers
      if (question.getAnswer() != null) {
        Content answer = (Content) question.getAnswer();
        if (answer.getChildren() != null) {
          for (ContentBase cb : answer.getChildren()) {
            Content c = (Content) cb;
            this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
          }
        }
      }

      if (question.getDefaultFeedback() != null) {
        Content defaultFeedback = question.getDefaultFeedback();
        if (defaultFeedback.getChildren() != null) {
          for (ContentBase cb : defaultFeedback.getChildren()) {
            Content c = (Content) cb;
            this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
          }
        }
      }

      if (content instanceof ChoiceQuestion) {
        ChoiceQuestion choiceQuestion = (ChoiceQuestion) content;
        if (choiceQuestion.getChoices() != null) {
          for (ContentBase cb : choiceQuestion.getChoices()) {
            Content c = (Content) cb;
            this.augmentChildContent(c, canonicalSourceFile, newParentId, parentPublished);
          }
        }
      }
    }

    // try to determine if we have media as fields to deal with in this class
    Method[] methods = content.getClass().getDeclaredMethods();
    for (Method method : methods) {
      if (Media.class.isAssignableFrom(method.getReturnType())) {
        try {
          Media media = (Media) method.invoke(content);
          if (media != null) {
            media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));
          }
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException
                 | InvocationTargetException e) {
          log.error("Unable to access method using reflection: attempting to fix Media Src", e);
        }
      }
    }

    if (content instanceof Media) {
      Media media = (Media) content;
      media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));

      // for tracking purposes we want to generate an id for all image content objects.
      if (media.getId() == null && media.getSrc() != null) {
        media.setId(parentId + Constants.ID_SEPARATOR
            + Base64.encodeBase64String(media.getSrc().getBytes()));
      }
    }

    // Concatenate the parentId with our id to get a fully qualified
    // identifier.
    if (content.getId() != null && parentId != null) {
      content.setId(parentId + Constants.ID_SEPARATOR + content.getId());
      content.setPublished(parentPublished);
    }

    return content;
  }

  private void collateSearchableContent(final Content content, final StringBuilder searchableContentBuilder) {
    if (null != content) {
      // Add the fields of interest to the string builder
      if (null != content.getTitle()) {
        searchableContentBuilder.append(content.getTitle()).append("\n");
      }
      if (null != content.getValue()) {
        searchableContentBuilder.append(content.getValue()).append("\n");
      }

      // Repeat the process for each child
      if (!content.getChildren().isEmpty()) {
        for (ContentBase childContentBase : content.getChildren()) {
          if (childContentBase instanceof Content) {
            Content child = (Content) childContentBase;
            this.collateSearchableContent(child, searchableContentBuilder);
          }
        }
      }
    }
  }

  /**
   * Concatenate the source of a media content object with its parent source file.
   *
   * @param canonicalSourceFile the canonical path to use for concat operations.
   * @param originalSrc         to modify
   * @return src with relative paths fixed.
   */
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
    Set<Content> setOfContentObjects = new HashSet<>();
    if (!content.getChildren().isEmpty()) {

      List<ContentBase> children = content.getChildren();

      for (ContentBase child : children) {
        setOfContentObjects.add((Content) child);
        setOfContentObjects.addAll(flattenContentObjects((Content) child));
      }
    }

    setOfContentObjects.add(content);

    return setOfContentObjects;
  }


  /**
   * Helper method to register problems with content objects.
   * - to which the problem relates
   *
   * @param c                 Partial content object to represent the object that has problems.
   * @param message           Error message to associate with the problem file / content.
   * @param indexProblemCache a map of problems found in the indexed content
   */
  private synchronized void registerContentProblem(final Content c, final String message,
                                                   final Map<Content, List<String>> indexProblemCache) {
    requireNonNull(c);

    // try and make sure each dummy content object has a title
    if (c.getTitle() == null) {
      c.setTitle(Paths.get(c.getCanonicalSourceFile()).getFileName().toString());
    }

    if (!indexProblemCache.containsKey(c)) {
      indexProblemCache.put(c, new ArrayList<>());
    }

    log.debug(message);
    indexProblemCache.get(c).add(message); //.replace("_", "\\_"));
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

    Set<String> newTagSet = Sets.newHashSet();

    // sanity check that tags are trimmed.
    for (String tag : tags) {
      newTagSet.add(tag.trim());
    }

    tagsList.addAll(newTagSet);
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

    HashMap<String, String> newUnits = Maps.newHashMap();

    for (Choice c : q.getChoices()) {
      if (c instanceof Quantity) {
        Quantity quantity = (Quantity) c;

        if (quantity.getUnits() != null && !quantity.getUnits().isEmpty()) {
          String units = quantity.getUnits();
          String cleanKey = units.replace("\t", "").replace("\n", "").replace(" ", "");

          // May overwrite previous entry, doesn't matter as there is
          // no mechanism by which to choose a winner
          newUnits.put(cleanKey, units);
        }
      }
    }

    if (newUnits.isEmpty()) {
      // This question contained no units.
      return;
    }

    allUnits.putAll(newUnits);
    if (q.getPublished()) {
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

    log.info("Building search indexes for: " + sanitiseInternalLogValue(sha));

    // setup object mapper to use pre-configured deserializer module.
    // Required to deal with type polymorphism
    List<Map.Entry<String, String>> contentToIndex = Lists.newArrayList();
    ObjectMapper objectMapper = mapperUtils.getSharedContentObjectMapper();
    for (Content content : gitCache.values()) {
      try {
        contentToIndex.add(immutableEntry(content.getId(), objectMapper.writeValueAsString(content)));
      } catch (JsonProcessingException e) {
        log.error("Unable to serialize content object: {} for indexing with the search provider.", content.getId(), e);
        this.registerContentProblem(content, "Search Index Error: " + content.getId()
            + content.getCanonicalSourceFile() + " Exception: " + e.toString(), indexProblemCache);
      }
    }

    long startTime;
    long endTime;

    try {
      es.indexObject(sha, ContentIndextype.METADATA.toString(),
          objectMapper.writeValueAsString(Map.of("version", sha, "created", Instant.now().toString())), "general");
      es.indexObject(sha, ContentIndextype.METADATA.toString(),
          objectMapper.writeValueAsString(Map.of("tags", tagsList)), "tags");

      startTime = System.nanoTime();
      es.bulkIndex(sha, ContentIndextype.UNIT.toString(), allUnits.entrySet().stream().map(entry -> {
        try {
          return objectMapper.writeValueAsString(Map.of("cleanKey", entry.getKey(), "unit", entry.getValue()));
        } catch (JsonProcessingException jsonProcessingException) {
          log.error("Unable to serialise unit entry for unit: {}", entry.getValue());
          return null;
        }
      }).filter(Objects::nonNull).collect(Collectors.toList()));
      es.bulkIndex(sha, ContentIndextype.PUBLISHED_UNIT.toString(), publishedUnits.entrySet().stream().map(entry -> {
        try {
          return objectMapper.writeValueAsString(Map.of("cleanKey", entry.getKey(), "unit", entry.getValue()));
        } catch (JsonProcessingException jsonProcessingException) {
          log.error("Unable to serialise published unit entry for unit: {}", entry.getValue());
          return null;
        }
      }).filter(Objects::nonNull).collect(Collectors.toList()));
      endTime = System.nanoTime();
      log.info("Bulk unit indexing took: {}ms", (endTime - startTime) / NANOSECONDS_IN_A_MILLISECOND);

      startTime = System.nanoTime();
      es.bulkIndex(sha, ContentIndextype.CONTENT_ERROR.toString(), indexProblemCache.entrySet().stream().map(e -> {
        try {
          return objectMapper.writeValueAsString(Map.of(
              "canonicalSourceFile", e.getKey().getCanonicalSourceFile(),
              "id", e.getKey().getId() == null ? "" : e.getKey().getId(),
              "title", e.getKey().getTitle() == null ? "" : e.getKey().getTitle(),
              // "tags", c.getTags(), // TODO: Add tags
              "published", e.getKey().getPublished() == null ? "" : e.getKey().getPublished(),
              "errors", e.getValue().toArray()));
        } catch (JsonProcessingException jsonProcessingException) {
          log.error("Unable to serialise content error entry from file: {}", e.getKey().getCanonicalSourceFile());
          return null;
        }
      }).filter(Objects::nonNull).collect(Collectors.toList()));
      endTime = System.nanoTime();
      log.info("Bulk content error indexing took: {}ms", (endTime - startTime) / NANOSECONDS_IN_A_MILLISECOND);
    } catch (JsonProcessingException e) {
      log.error("Unable to serialise sha or tags");
    } catch (SegueSearchException e) {
      log.error("Unable to index sha, tags, units or content errors.");
    }


    try {
      startTime = System.nanoTime();
      es.bulkIndexWithIds(sha, ContentIndextype.CONTENT.toString(), contentToIndex);
      endTime = System.nanoTime();
      log.info("Bulk indexing content took: {}ms", (endTime - startTime) / NANOSECONDS_IN_A_MILLISECOND);
      log.info("Search index request sent for: " + sanitiseInternalLogValue(sha));
    } catch (SegueSearchException e) {
      log.error("Error whilst trying to perform bulk index operation.", e);
    } catch (ActionRequestValidationException e) {
      log.error("Error validating content during index", e);
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

    Set<Content> allObjectsSeen = new HashSet<>();
    Set<String> expectedIds = new HashSet<>();
    Map<String, Content> contentById = new HashMap<>();
    Map<String, Set<Content>> incomingReferences = new HashMap<>(); // my id -> set of who references me

    // Build up a set of all content (and content fragments for validation)
    for (Content c : gitCache.values()) {
      allObjectsSeen.addAll(this.flattenContentObjects(c));
    }

    // Start looking for issues in the flattened content data
    for (Content c : allObjectsSeen) {
      // add the id to the list of defined ids
      if (c.getId() != null) {
        contentById.put(c.getId(), c);
      }

      // add the ids to the list of expected ids
      if (c.getRelatedContent() != null) {
        expectedIds.addAll(c.getRelatedContent());
        // record which content object was referencing which ID
        for (String id : c.getRelatedContent()) {
          if (!incomingReferences.containsKey(id)) {
            incomingReferences.put(id, new HashSet<>());
          }
          incomingReferences.get(id).add(c);
        }
      }

      // content type specific checks
      try {
        this.recordContentTypeSpecificError(sha, c, indexProblemCache);
      } catch (NullPointerException e) {
        log.warn("Failed processing content errors in file: {}", c.getCanonicalSourceFile());
      }
    }

    // Find all references to missing content.
    Set<String> missingContent = new HashSet<>(expectedIds);
    missingContent.removeAll(contentById.keySet());

    for (String id : missingContent) {
      for (Content src : incomingReferences.get(id)) {
        this.registerContentProblem(src, "The id '" + id + "' was referenced by "
            + src.getCanonicalSourceFile() + " but the content with that "
            + "ID cannot be found.", indexProblemCache);
      }
    }
    if (!missingContent.isEmpty()) {
      log.debug("Referential integrity broken for ({}) related Content items. "
          + "The following ids are referenced but do not exist: {}", missingContent.size(), expectedIds);
    }

    // Find all references from published content to unpublished content.
    for (String refTargetId : incomingReferences.keySet()) {
      Content refTarget = contentById.get(refTargetId);
      if (refTarget != null) {
        for (Content refSrc : incomingReferences.get(refTargetId)) {
          if (refSrc.getPublished() && !refTarget.getPublished()) {
            this.registerContentProblem(refSrc, "Content is published, "
                + "but references unpublished content '" + refTargetId + "'.", indexProblemCache);
          }
        }
      }
    }

    log.info(String.format("Validation processing (%s) complete. There are %s files with content problems",
        sanitiseInternalLogValue(sha), indexProblemCache.size()));

    if (indexProblemCache.isEmpty()) {
      // Register a no-op style error to simplify application logic by ensuring there is always a content errors index
      Content dummyContentRecord = new Content();
      // "\uD83D\uDE0E"
      dummyContentRecord.setCanonicalSourceFile("😎");

      this.registerContentProblem(dummyContentRecord, "No content errors!", indexProblemCache);
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
    log.info("Deleting existing indexes for version " + sanitiseInternalLogValue(version));
    for (ContentIndextype contentIndexType : ContentIndextype.values()) {
      es.expungeIndexFromSearchCache(version, contentIndexType.toString());
    }
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
    StringBuilder ret = new StringBuilder();
    for (Content child : flattenContentObjects(content)) {
      if (child != content && null != child.getExpandable() && child.getExpandable()) {
        ret.append(null != child.getType() ? child.getType() : "undefined").append(",");
      }
    }
    if (ret.length() > 0) {
      ret.deleteCharAt(ret.length() - 1);
    }
    return ret.toString();
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
    // ensure content does not have children and a value
    registerContentProblemValueWithChildren(content, indexProblemCache);

    // Make sure no children of potentially expandable content are expandable, if so record a content error
    registerContentProblemNestedExpandables(content, indexProblemCache);

    // Ensure that the expandable content is only of a type that support expansion
    registerContentProblemUnsupportedTypeExpandable(content, indexProblemCache);

    registerContentProblemsMediaInvalidProperties(sha, content, indexProblemCache);

    registerContentProblemQuestionMissingId(content, indexProblemCache);

    registerContentProblemsChoiceQuestionMissingChoicesOrAnswer(content, indexProblemCache);

    registerContentProblemEmailTemplateMissingPainTextContentField(content, indexProblemCache);

    registerContentProblemEventMissingOrInvalidEndDate(content, indexProblemCache);

    // Find quantities with values that cannot be parsed as numbers.
    registerContentProblemsNumericQuestionInvalidChoicesOrUnits(content, indexProblemCache);

    // Find Symbolic Questions with broken properties.
    if (content instanceof IsaacSymbolicQuestion) {
      if (content.getClass().equals(IsaacSymbolicQuestion.class)) {
        registerContentProblemsSymbolicQuestionInvalidProperties(content, indexProblemCache);
      }
    }

    registerContentProblemsClozeQuestionChoicesHaveWrongNumberOFItems(content, indexProblemCache);
  }

  private void registerContentProblemsClozeQuestionChoicesHaveWrongNumberOFItems(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content instanceof IsaacClozeQuestion) {
      IsaacClozeQuestion q = (IsaacClozeQuestion) content;
      Integer numberItems = null;
      for (Choice choice : q.getChoices()) {
        if (choice instanceof ItemChoice) {
          ItemChoice c = (ItemChoice) choice;
          if (null == c.getItems() || c.getItems().isEmpty()) {
            this.registerContentProblem(content, "Cloze Question: " + q.getId() + " has choice with missing items!",
                indexProblemCache);
            continue;
          }
          int items = c.getItems().size();
          if (numberItems != null && items != numberItems) {
            this.registerContentProblem(content,
                "Cloze Question: " + q.getId() + " has choice with incorrect number of items!"
                    + " (Expected " + numberItems + ", got " + items + "!)", indexProblemCache);
            continue;
          }
          numberItems = items;
        }
      }
    }
  }

  private void registerContentProblemsSymbolicQuestionInvalidProperties(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    IsaacSymbolicQuestion question = (IsaacSymbolicQuestion) content;
    for (String sym : question.getAvailableSymbols()) {
      registerContentProblemQuestionSymbolContainsBackslash(content, indexProblemCache, question, sym);
    }
    for (Choice choice : question.getChoices()) {
      if (choice instanceof Formula) {
        Formula f = (Formula) choice;
        if (f.getPythonExpression().contains("\\")) {
          registerContentProblemQuestionFormulaContainsBackslash(content, indexProblemCache, question, choice);
        } else if (f.getPythonExpression() == null || f.getPythonExpression().isEmpty()) {
          registerContentProblemQuestionFormulaIsEmpty(content, indexProblemCache, question, choice);
        }
      } else {
        registerContentProblemSymbolicQuestionChoiceIsNotFormula(content, indexProblemCache, question, choice);
      }
    }
  }

  private void registerContentProblemSymbolicQuestionChoiceIsNotFormula(
      final Content content, final Map<Content, List<String>> indexProblemCache, final IsaacSymbolicQuestion question,
      final Choice choice) {
    this.registerContentProblem(content, "Symbolic Question: " + question.getId() + " has non-Formula Choice ("
        + choice.getValue() + "). It must be deleted and a new Formula Choice created.", indexProblemCache);
  }

  private void registerContentProblemQuestionFormulaIsEmpty(
      final Content content, final Map<Content, List<String>> indexProblemCache, final IsaacSymbolicQuestion question,
      final Choice choice) {
    this.registerContentProblem(content, "Symbolic Question: " + question.getId() + " has Formula ("
        + choice.getValue() + ") with empty pythonExpression!", indexProblemCache);
  }

  private void registerContentProblemQuestionFormulaContainsBackslash(
      final Content content, final Map<Content, List<String>> indexProblemCache, final IsaacSymbolicQuestion question,
      final Choice choice) {
    this.registerContentProblem(content, "Symbolic Question: " + question.getId() + " has Formula ("
        + choice.getValue() + ") with pythonExpression which contains a '\\' character.", indexProblemCache);
  }

  private void registerContentProblemQuestionSymbolContainsBackslash(
      final Content content, final Map<Content, List<String>> indexProblemCache, final IsaacSymbolicQuestion question,
      final String sym) {
    if (sym.contains("\\")) {
      this.registerContentProblem(content, "Symbolic Question: " + question.getId() + " has availableSymbol ("
          + sym + ") which contains a '\\' character.", indexProblemCache);
    }
  }

  private void registerContentProblemsNumericQuestionInvalidChoicesOrUnits(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content instanceof IsaacNumericQuestion) {
      IsaacNumericQuestion question = (IsaacNumericQuestion) content;
      for (Choice choice : question.getChoices()) {
        if (choice instanceof Quantity) {
          Quantity quantity = (Quantity) choice;

          registerContentProblemCannotParseQuantityChoiceAsNumber(content, indexProblemCache, question, quantity);

          registerContentProblemUnnecessaryQuantityChoiceUnits(content, indexProblemCache, question, quantity);


        } else {
          registerContentProblemNumericQuestionChoiceIsNotQuantity(content, indexProblemCache, question, choice);
        }
      }
      registerContentProblemConflictingUnitSettings(content, indexProblemCache, question);
    }
  }

  private void registerContentProblemConflictingUnitSettings(
      final Content content, final Map<Content, List<String>> indexProblemCache, final IsaacNumericQuestion question) {
    if (question.getRequireUnits() && null != question.getDisplayUnit() && !question.getDisplayUnit().isEmpty()) {
      this.registerContentProblem(content,
          "Numeric Question: " + question.getId() + " has a displayUnit set but also requiresUnits!"
              + " Units will be ignored for this question!", indexProblemCache);
    }
  }

  private void registerContentProblemNumericQuestionChoiceIsNotQuantity(
      final Content content, final Map<Content, List<String>> indexProblemCache, final IsaacNumericQuestion question,
      final Choice choice) {
    this.registerContentProblem(content, "Numeric Question: " + question.getId() + " has non-Quantity Choice ("
        + choice.getValue() + "). It must be deleted and a new Quantity Choice created.", indexProblemCache);
  }

  private void registerContentProblemUnnecessaryQuantityChoiceUnits(
      final Content content, final Map<Content, List<String>> indexProblemCache, final IsaacNumericQuestion question,
      final Quantity quantity) {
    if (!question.getRequireUnits() && null != quantity.getUnits() && !quantity.getUnits().isEmpty()) {
      this.registerContentProblem(content, "Numeric Question: " + question.getId()
          + " has a Quantity with units but does not require units!", indexProblemCache);
    }
  }

  private void registerContentProblemCannotParseQuantityChoiceAsNumber(
      final Content content, final Map<Content, List<String>> indexProblemCache, final IsaacNumericQuestion question,
      final Quantity quantity) {
    // Check valid number by parsing in the same way as IsaacNumericValidator::stringValueToDouble:
    try {
      new BigDecimal(quantity.getValue());
    } catch (NumberFormatException e) {
      this.registerContentProblem(content,
          "Numeric Question: " + question.getId() + " has Quantity (" + quantity.getValue()
              + ")  with value that cannot be interpreted as a number. "
              + "Users will never be able to match this answer.", indexProblemCache);
    }
  }

  private void registerContentProblemEventMissingOrInvalidEndDate(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content instanceof IsaacEventPage eventPage) {
      if (eventPage.getEndDate() == null) {
        this.registerContentProblem(content, "Event has no end date", indexProblemCache);
      } else if (eventPage.getEndDate().isBefore(eventPage.getDate())) {
        this.registerContentProblem(content, "Event has end date before start date", indexProblemCache);
      }
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
    if (content instanceof ChoiceQuestion question && !(content.getType().equals("isaacQuestion"))) {

      if (question.getChoices() == null || question.getChoices().isEmpty()) {
        registerContentProblemChoiceQuestionMissingChoices(indexProblemCache, question);
      } else {
        registerContentProblemChoiceQuestionMissingAnswer(indexProblemCache, question);
      }
    }
  }

  private void registerContentProblemChoiceQuestionMissingAnswer(
      final Map<Content, List<String>> indexProblemCache, final ChoiceQuestion question) {
    boolean correctOptionFound = false;
    for (Choice choice : question.getChoices()) {
      if (choice.isCorrect()) {
        correctOptionFound = true;
      }
    }
    if (!correctOptionFound) {
      this.registerContentProblem(question,
          "Question: " + question.getId() + " found without a correct answer. "
              + "This question will always be automatically marked as incorrect", indexProblemCache);
    }
  }

  private void registerContentProblemChoiceQuestionMissingChoices(
      final Map<Content, List<String>> indexProblemCache, final ChoiceQuestion question) {
    this.registerContentProblem(question,
        "Question: " + question.getId() + " found without any choice metadata. "
            + "This question will always be automatically " + "marked as incorrect", indexProblemCache);
  }

  private void registerContentProblemQuestionMissingId(
      final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content instanceof Question && content.getId() == null) {
      this.registerContentProblem(content, "Question: " + content.getTitle() + " in " + content.getCanonicalSourceFile()
          + " found without a unique id. " + "This question cannot be logged correctly.", indexProblemCache);
    }
  }

  private void registerContentProblemsMediaInvalidProperties(
      final String sha, final Content content, final Map<Content, List<String>> indexProblemCache) {
    if (content instanceof Media) {
      Media media = (Media) content;

      registerContentProblemMediaNotFoundOrTooLarge(sha, content, indexProblemCache, media);

      // check that there is some alt text.
      registerContentProblemMediaMissingAltText(content, indexProblemCache, media);
    }
  }

  private void registerContentProblemMediaMissingAltText(
      final Content content, final Map<Content, List<String>> indexProblemCache, final Media media) {
    if (media.getAltText() == null || media.getAltText().isEmpty()) {
      if (!(media instanceof Video) && !media.getId().equals("eventThumbnail")) {
        // Videos probably don't need alt text unless there is a good reason. It's not important that event
        // thumbnails have alt text, so we don't record errors for those either.
        this.registerContentProblem(content, "No altText attribute set for media element: " + media.getSrc()
            + " in Git source file " + content.getCanonicalSourceFile(), indexProblemCache);
      }
    }
  }

  private void registerContentProblemMediaNotFoundOrTooLarge(
      final String sha, final Content content, final Map<Content, List<String>> indexProblemCache, final Media media) {
    if (media.getSrc() != null && !media.getSrc().startsWith("http")) {
      ByteArrayOutputStream fileData;
      try {
        // This will return null if the file is not found:
        fileData = database.getFileByCommitSha(sha, media.getSrc());
      } catch (IOException | UnsupportedOperationException e) {
        fileData = null;
      }
      if (null == fileData) {
        this.registerContentProblem(content, "Unable to find Image: " + media.getSrc()
                + " in Git. Could the reference be incorrect? SourceFile is " + content.getCanonicalSourceFile(),
            indexProblemCache);
      } else if (fileData.size() > MEDIA_FILE_SIZE_LIMIT) {
        int sizeInKiloBytes = fileData.size() / BYTES_IN_ONE_KILOBYTE;
        this.registerContentProblem(content, String.format("Image (%s) is %s kB and exceeds file size warning limit!",
            media.getSrc(), sizeInKiloBytes), indexProblemCache);
      }
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
    if (content.getValue() != null && !content.getChildren().isEmpty()) {
      String id = content.getId();
      String firstLine = "Content";
      if (id != null) {
        firstLine += ": " + id;
      }

      this.registerContentProblem(content, firstLine + " in " + content.getCanonicalSourceFile()
          + " found with both children and a value. "
          + "Content objects are only allowed to have one or the other.", indexProblemCache);

      log.error(
          "Invalid content item detected: The object with ID ({}) has both children and a value.",
          content.getCanonicalSourceFile()
      );
    }
  }
}
