/**
 * Copyright 2014 Stephen Cummins and Ian Davies
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.dao.content;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.HIDE_FROM_FILTER_TAG;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.PAGE_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.SEARCHABLE_TAG;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.SITE_WIDE_SEARCH_VALID_DOC_TYPES;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.TOPIC_SUMMARY_PAGE_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_CACHE_EXPIRE_AFTER_ACCESS_DAYS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_SHA_CACHE_EXPIRE_AFTER_ACCESS_SECONDS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ContentIndextype;
import static uk.ac.cam.cl.dtg.segue.api.Constants.IMPORTANT_DOCUMENT_TYPE_BOOST;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MATCH_INSTRUCTION_ADDRESS_FUZZY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MATCH_INSTRUCTION_ADDRESS_NON_FUZZY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MATCH_INSTRUCTION_IMPORTANT_FUZZY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MATCH_INSTRUCTION_IMPORTANT_NON_FUZZY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MATCH_INSTRUCTION_OTHER_FUZZY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.MATCH_INSTRUCTION_OTHER_NON_FUZZY;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.CACHE_METRICS_COLLECTOR;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.api.client.util.Sets;
import com.google.common.base.Functions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.ContentMapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.BasicSearchParameters;
import uk.ac.cam.cl.dtg.segue.search.BooleanMatchInstruction;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.MustMatchInstruction;
import uk.ac.cam.cl.dtg.segue.search.RangeMatchInstruction;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.segue.search.ShouldMatchInstruction;
import uk.ac.cam.cl.dtg.segue.search.SimpleExclusionInstruction;
import uk.ac.cam.cl.dtg.segue.search.SimpleFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.TermsFilterInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Implementation that specifically works with Content objects.
 */
public class GitContentManager {
  private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);

  private static final String CONTENT_TYPE = "content";

  private final GitDb database;
  private final ContentMapperUtils mapperUtils;
  private final ContentMapper objectMapper;
  private final ISearchProvider searchProvider;
  private final PropertiesLoader globalProperties;
  private final boolean allowOnlyPublishedContent;
  private final boolean hideRegressionTestContent;

  private final Cache<Object, Object> cache;
  private final Cache<String, GetResponse> contentShaCache;

  private final String contentIndex;


  /**
   * Constructor for instantiating a new Git Content Manager Object.
   *
   * @param database           - that the content Manager manages.
   * @param searchProvider     - search provider that the content manager manages and controls.
   * @param contentMapperUtils - The utility class for mapping content objects.
   * @param globalProperties   - global properties.
   */
  @Inject
  public GitContentManager(final GitDb database, final ISearchProvider searchProvider,
                           final ContentMapperUtils contentMapperUtils, final ContentMapper objectMapper,
                           final PropertiesLoader globalProperties) {
    this.database = database;
    this.mapperUtils = contentMapperUtils;
    this.objectMapper = objectMapper;
    this.searchProvider = searchProvider;
    this.globalProperties = globalProperties;

    this.allowOnlyPublishedContent = Boolean.parseBoolean(
        globalProperties.getProperty(Constants.SHOW_ONLY_PUBLISHED_CONTENT));
    if (this.allowOnlyPublishedContent) {
      log.info("API Configured to only allow published content to be returned.");
    }

    this.hideRegressionTestContent = Boolean.parseBoolean(
        globalProperties.getProperty(Constants.HIDE_REGRESSION_TEST_CONTENT));
    if (this.hideRegressionTestContent) {
      log.info("API Configured to hide content tagged with 'regression_test'.");
    }

    this.cache = CacheBuilder.newBuilder().recordStats().softValues()
        .expireAfterAccess(CONTENT_CACHE_EXPIRE_AFTER_ACCESS_DAYS, TimeUnit.DAYS).build();
    CACHE_METRICS_COLLECTOR.addCache("git_content_manager_cache", cache);

    this.contentShaCache = CacheBuilder.newBuilder().softValues()
        .expireAfterWrite(CONTENT_SHA_CACHE_EXPIRE_AFTER_ACCESS_SECONDS, TimeUnit.SECONDS).build();

    this.contentIndex = globalProperties.getProperty(Constants.CONTENT_INDEX);
  }

  /**
   * FOR TESTING PURPOSES ONLY - Constructor for instantiating a new Git Content Manager Object.
   *
   * @param database           - that the content Manager manages.
   * @param searchProvider     - search provider that the content manager manages and controls.
   * @param contentMapperUtils - The utility class for mapping content objects.
   */
  public GitContentManager(final GitDb database, final ISearchProvider searchProvider,
                           final ContentMapperUtils contentMapperUtils, final ContentMapper objectMapper) {
    this.database = database;
    this.mapperUtils = contentMapperUtils;
    this.objectMapper = objectMapper;
    this.searchProvider = searchProvider;
    this.globalProperties = null;
    this.allowOnlyPublishedContent = false;
    this.hideRegressionTestContent = false;
    this.cache = CacheBuilder.newBuilder().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
    this.contentShaCache = CacheBuilder.newBuilder().softValues().expireAfterWrite(1, TimeUnit.MINUTES).build();
    this.contentIndex = null;
  }

  /**
   * Get a DTO object by its ID or return null.
   * <br>
   * This may return a cached object, and will temporarily cache the object.
   * Do not modify the returned DTO object.
   * The object will be retrieved in DO form, and mapped to a DTO. Both versions will be
   * locally cached to avoid re-querying the data store and the deserialization costs.
   *
   * @param id the content object ID.
   * @return the content DTO object.
   * @throws ContentManagerException on failure to return the object or null.
   */
  public final ContentDTO getContentById(final String id) throws ContentManagerException {
    return getContentById(id, false);
  }

  /**
   * Get a DTO object by its ID or return null.
   * <br>
   * This may return a cached object, and will temporarily cache the object.
   * Do not modify the returned DTO object.
   * The object will be retrieved in DO form, and mapped to a DTO. Both versions will be
   * locally cached to avoid re-querying the data store and the deserialization costs.
   *
   * @param id          the content object ID.
   * @param failQuietly whether to log a warning if the content cannot be found.
   * @return the content DTO object.
   * @throws ContentManagerException on failure to return the object or null.
   */
  public ContentDTO getContentById(final String id, final boolean failQuietly) throws ContentManagerException {
    String k = "getContentById~" + getCurrentContentSHA() + "~" + id;
    if (!cache.asMap().containsKey(k)) {
      ContentDTO c = this.mapperUtils.getDTOByDO(this.getContentDOById(id, failQuietly));
      if (c != null) {
        cache.put(k, c);
      }
    }

    return (ContentDTO) cache.getIfPresent(k);
  }

  /**
   * Get a DO object by its ID or return null.
   * <br>
   * This may return a cached object, and will temporarily cache the object
   * to avoid re-querying the data store and the deserialization costs.
   * Do not modify the returned DO object.
   *
   * @param id the content object ID.
   * @return the content DTO object.
   * @throws ContentManagerException on failure to return the object or null.
   */
  public final Content getContentDOById(final String id) throws ContentManagerException {
    return getContentDOById(id, false);
  }

  /**
   * Get a DO object by its ID or return null.
   * <br>
   * This may return a cached object, and will temporarily cache the object
   * to avoid re-querying the data store and the deserialization costs.
   * Do not modify the returned DO object.
   *
   * @param id          the content object ID.
   * @param failQuietly whether to log a warning if the content cannot be found.
   * @return the content DTO object.
   * @throws ContentManagerException on failure to return the object or null.
   */
  public final Content getContentDOById(final String id, final boolean failQuietly) throws ContentManagerException {
    if (id == null || id.isEmpty()) {
      return null;
    }

    String k = "getContentDOById~" + getCurrentContentSHA() + "~" + id;
    if (!cache.asMap().containsKey(k)) {

      List<Content> searchResults = mapperUtils.mapFromStringListToContentList(this.searchProvider.termSearch(
          new BasicSearchParameters(contentIndex, CONTENT_TYPE, 0, 1), id,
          Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
          this.getBaseFilters()).getResults()
      );

      if (null == searchResults || searchResults.isEmpty()) {
        if (!failQuietly) {
          log.error(String.format("Failed to locate content with ID '%s' in the cache for content SHA (%s)",
                            sanitiseExternalLogValue(id),
              getCurrentContentSHA()));
        }
        return null;
      }

      cache.put(k, searchResults.get(0));
    }

    return (Content) cache.getIfPresent(k);

  }

  /**
   * Retrieve all DTO content matching an ID prefix.
   * <br>
   * This may return cached objects, and will temporarily cache the objects
   * to avoid re-querying the data store and the deserialization costs.
   * Do not modify the returned DTO objects.
   *
   * @param idPrefix   the content object ID prefix.
   * @param startIndex the integer start index for pagination.
   * @param limit      the limit for pagination.
   * @return a ResultsWrapper of the matching content.
   * @throws ContentManagerException on failure to return the objects.
   */
  public ResultsWrapper<ContentDTO> getByIdPrefix(final String idPrefix, final int startIndex,
                                                  final int limit) throws ContentManagerException {

    String k = "getByIdPrefix~" + getCurrentContentSHA() + "~" + idPrefix + "~" + startIndex + "~" + limit;
    if (!cache.asMap().containsKey(k)) {

      ResultsWrapper<String> searchHits = this.searchProvider.findByPrefix(
          new BasicSearchParameters(contentIndex, CONTENT_TYPE, startIndex, limit),
          Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
          idPrefix, this.getBaseFilters());

      List<Content> searchResults = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

      cache.put(k, new ResultsWrapper<>(mapperUtils.getDTOByDOList(searchResults), searchHits.getTotalResults()));
    }

    return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
  }

  /**
   * Get a list of DTO objects by their IDs.
   * <br>
   * This may return cached objects, and will temporarily cache the objects
   * to avoid re-querying the data store and the deserialization costs.
   * Do not modify the returned DTO objects.
   *
   * @param ids        the list of content object IDs.
   * @param startIndex the integer start index for pagination.
   * @param limit      the limit for pagination.
   * @return a ResultsWrapper of the matching content.
   * @throws ContentManagerException on failure to return the objects.
   */
  public ResultsWrapper<ContentDTO> getContentMatchingIds(final Collection<String> ids,
                                                          final int startIndex, final int limit)
      throws ContentManagerException {

    String k =
        "getContentMatchingIds~" + getCurrentContentSHA() + "~" + ids.toString() + "~" + startIndex + "~" + limit;
    if (!cache.asMap().containsKey(k)) {

      Map<String, AbstractFilterInstruction> finalFilter = Maps.newHashMap();
      finalFilter.putAll(new ImmutableMap.Builder<String, AbstractFilterInstruction>()
          .put(Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
              new TermsFilterInstruction(ids))
          .build());

      if (getBaseFilters() != null) {
        finalFilter.putAll(getBaseFilters());
      }

      ResultsWrapper<String> searchHits = this.searchProvider.termSearch(
          new BasicSearchParameters(contentIndex, CONTENT_TYPE, startIndex, limit),
          null,
          null,
          finalFilter
      );

      List<Content> searchResults = mapperUtils.mapFromStringListToContentList(searchHits.getResults());
      cache.put(k, new ResultsWrapper<>(mapperUtils.getDTOByDOList(searchResults), searchHits.getTotalResults()));
    }

    return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
  }

  public final ResultsWrapper<ContentDTO> searchForContent(
      final String searchString, @Nullable final Map<String, List<String>> fieldsThatMustMatch,
      final Integer startIndex, final Integer limit) throws ContentManagerException {

    ResultsWrapper<String> searchHits = searchProvider.fuzzySearch(
        new BasicSearchParameters(contentIndex, CONTENT_TYPE, startIndex, limit),
        searchString,
        fieldsThatMustMatch,
        this.getBaseFilters(),
        Constants.ID_FIELDNAME,
        Constants.TITLE_FIELDNAME,
        Constants.TAGS_FIELDNAME,
        Constants.VALUE_FIELDNAME,
        Constants.CHILDREN_FIELDNAME
    );

    List<Content> searchResults = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

    return new ResultsWrapper<>(mapperUtils.getDTOByDOList(searchResults), searchHits.getTotalResults());
  }

  public final ResultsWrapper<ContentDTO> siteWideSearch(
      final String searchString, final List<String> documentTypes,
      final boolean includeHiddenContent, final Integer startIndex, final Integer limit
  ) throws ContentManagerException {
    String nestedFieldConnector = searchProvider.getNestedFieldConnector();

    List<String> importantDocumentTypes = List.of(TOPIC_SUMMARY_PAGE_TYPE);

    List<String> importantFields = List.of(
        Constants.TITLE_FIELDNAME, Constants.ID_FIELDNAME, Constants.SUMMARY_FIELDNAME, Constants.TAGS_FIELDNAME
    );
    List<String> otherFields = List.of(Constants.SEARCHABLE_CONTENT_FIELDNAME);

    BooleanMatchInstruction matchQuery = new BooleanMatchInstruction();
    int numberOfExpectedShouldMatches = 1;

    List<String> validDocumentTypes = Optional.ofNullable(documentTypes).orElse(Collections.emptyList()).stream()
        .filter(SITE_WIDE_SEARCH_VALID_DOC_TYPES::contains).collect(Collectors.toList());
    if (validDocumentTypes.isEmpty()) {
      validDocumentTypes = Lists.newArrayList(SITE_WIDE_SEARCH_VALID_DOC_TYPES);
    }

    for (String documentType : validDocumentTypes) {
      BooleanMatchInstruction contentQuery = new BooleanMatchInstruction();

      // Match document type
      contentQuery.must(new MustMatchInstruction(Constants.TYPE_FIELDNAME, documentType));

      // Generic pages must be explicitly tagged to appear in search results
      if (documentType.equals(PAGE_TYPE)) {
        contentQuery.must(new MustMatchInstruction(Constants.TAGS_FIELDNAME, SEARCHABLE_TAG));
      }

      // Try to match fields
      for (String field : importantFields) {
        contentQuery.should(
            new ShouldMatchInstruction(field, searchString, MATCH_INSTRUCTION_IMPORTANT_NON_FUZZY, false));
        contentQuery.should(new ShouldMatchInstruction(field, searchString, MATCH_INSTRUCTION_IMPORTANT_FUZZY, true));
      }
      for (String field : otherFields) {
        contentQuery.should(new ShouldMatchInstruction(field, searchString, MATCH_INSTRUCTION_OTHER_NON_FUZZY, false));
        contentQuery.should(new ShouldMatchInstruction(field, searchString, MATCH_INSTRUCTION_OTHER_FUZZY, true));
      }

      // Check location.address fields on event pages
      if (documentType.equals(EVENT_TYPE)) {
        String addressPath = String.join(nestedFieldConnector, Constants.ADDRESS_PATH_FIELDNAME);
        for (String addressField : Constants.ADDRESS_FIELDNAMES) {
          String field = addressPath + nestedFieldConnector + addressField;
          contentQuery.should(
              new ShouldMatchInstruction(field, searchString, MATCH_INSTRUCTION_ADDRESS_NON_FUZZY, false));
          contentQuery.should(new ShouldMatchInstruction(field, searchString, MATCH_INSTRUCTION_ADDRESS_FUZZY, true));
        }
      }

      // Only show future events
      if (documentType.equals(EVENT_TYPE)) {
        LocalDate today = LocalDate.now();
        long now = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * Constants.EVENT_DATE_EPOCH_MULTIPLIER;
        contentQuery.must(new RangeMatchInstruction<Long>(Constants.DATE_FIELDNAME).greaterThanOrEqual(now));
      }

      contentQuery.setMinimumShouldMatch(numberOfExpectedShouldMatches);

      if (importantDocumentTypes.contains(documentType)) {
        contentQuery.setBoost(IMPORTANT_DOCUMENT_TYPE_BOOST);
      }

      matchQuery.should(contentQuery);
    }

    if (!includeHiddenContent) {
      // Do not include any content with a nofilter tag
      matchQuery.mustNot(new MustMatchInstruction(Constants.TAGS_FIELDNAME, HIDE_FROM_FILTER_TAG));
    }

    ResultsWrapper<String> searchHits = searchProvider.nestedMatchSearch(
        new BasicSearchParameters(contentIndex, CONTENT_TYPE, startIndex, limit),
        searchString,
        matchQuery,
        this.getBaseFilters()
    );

    List<Content> searchResults = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

    return new ResultsWrapper<>(mapperUtils.getDTOByDOList(searchResults), searchHits.getTotalResults());
  }

  public final ResultsWrapper<ContentDTO> findByFieldNames(
      final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex, final Integer limit
  ) throws ContentManagerException {
    return this.findByFieldNames(fieldsToMatch, startIndex, limit, null);
  }

  public final ResultsWrapper<ContentDTO> findByFieldNames(
      final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex,
      final Integer limit, @Nullable final Map<String, Constants.SortOrder> sortInstructions
  ) throws ContentManagerException {
    return this.findByFieldNames(fieldsToMatch, startIndex, limit, sortInstructions, null);
  }

  public final ResultsWrapper<ContentDTO> findByFieldNames(
      final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex, final Integer limit,
      @Nullable final Map<String, Constants.SortOrder> sortInstructions,
      @Nullable final Map<String, AbstractFilterInstruction> filterInstructions
  ) throws ContentManagerException {
    ResultsWrapper<ContentDTO> finalResults;

    final Map<String, Constants.SortOrder> newSortInstructions;
    if (null == sortInstructions || sortInstructions.isEmpty()) {
      newSortInstructions = Maps.newHashMap();
      newSortInstructions.put(Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
          Constants.SortOrder.ASC);
    } else {
      newSortInstructions = sortInstructions;
    }

    // add base filters to filter instructions
    Map<String, AbstractFilterInstruction> newFilterInstructions = filterInstructions;
    if (this.getBaseFilters() != null) {
      if (null == newFilterInstructions) {
        newFilterInstructions = Maps.newHashMap();
      }
      newFilterInstructions.putAll(this.getBaseFilters());
    }

    ResultsWrapper<String> searchHits = searchProvider.matchSearch(
        new BasicSearchParameters(contentIndex, CONTENT_TYPE, startIndex, limit),
        fieldsToMatch, newSortInstructions, newFilterInstructions);

    // setup object mapper to use pre-configured deserializer module.
    // Required to deal with type polymorphism
    List<Content> result = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

    List<ContentDTO> contentDTOResults = mapperUtils.getDTOByDOList(result);

    finalResults = new ResultsWrapper<>(contentDTOResults, searchHits.getTotalResults());

    return finalResults;
  }

  public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
      final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex,
      final Integer limit
  ) throws ContentManagerException {
    return this.findByFieldNamesRandomOrder(fieldsToMatch, startIndex, limit, null);
  }

  public ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
      final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex,
      final Integer limit, @Nullable final Long randomSeed
  ) throws ContentManagerException {
    ResultsWrapper<ContentDTO> finalResults;

    ResultsWrapper<String> searchHits;
    searchHits = searchProvider.randomisedMatchSearch(
        new BasicSearchParameters(contentIndex, CONTENT_TYPE, startIndex, limit), fieldsToMatch, randomSeed,
        this.getBaseFilters());

    // setup object mapper to use pre-configured deserializer module.
    // Required to deal with type polymorphism
    List<Content> result = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

    List<ContentDTO> contentDTOResults = mapperUtils.getDTOByDOList(result);

    finalResults = new ResultsWrapper<>(contentDTOResults, searchHits.getTotalResults());

    return finalResults;
  }

  public final ByteArrayOutputStream getFileBytes(final String filename) throws IOException {
    return database.getFileByCommitSha(getCurrentContentSHA(), filename);
  }

  public final String getLatestContentSHA() {
    return database.fetchLatestFromRemote();
  }

  public final Set<String> getCachedContentSHAList() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (String index : this.searchProvider.getAllIndices()) {
      // check to see if index looks like a content sha otherwise we will get loads of other search indexes come
      // back.
      if (index.matches("[a-fA-F0-9]{40}_.*")) {
        // We just want the commit SHA, not the type description after the underscore:
        builder.add(index.replaceAll("_.*$", ""));
      }
    }
    return builder.build();
  }

  public final Set<String> getTagsList() {
    try {
      List<Object> tagObjects = (List<Object>) searchProvider.getById(
          contentIndex,
          ContentIndextype.METADATA.toString(),
          "tags"
      ).getSource().get("tags");
      return new HashSet<>(Lists.transform(tagObjects, Functions.toStringFunction()));
    } catch (SegueSearchException e) {
      log.error("Failed to retrieve tags from search provider", e);
      return Sets.newHashSet();
    }
  }

  public final Collection<String> getAllUnits() {
    String unitType = ContentIndextype.UNIT.toString();
    if (globalProperties.getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(Constants.EnvironmentType.PROD.name())) {
      unitType = ContentIndextype.PUBLISHED_UNIT.toString();
    }
    try {
      SearchResponse r =
          searchProvider.getAllFromIndex(globalProperties.getProperty(Constants.CONTENT_INDEX), unitType);
      SearchHits hits = r.getHits();
      ArrayList<String> units = new ArrayList<>((int) hits.getTotalHits().value);
      for (SearchHit hit : hits) {
        units.add((String) hit.getSourceAsMap().get("unit"));
      }

      return units;
    } catch (SegueSearchException e) {
      log.error("Failed to retrieve all units from search provider", e);
      return Collections.emptyList();
    }
  }

  public final Map<Content, List<String>> getProblemMap() {
    try {
      SearchResponse r = searchProvider.getAllFromIndex(contentIndex,
          ContentIndextype.CONTENT_ERROR.toString());
      SearchHits hits = r.getHits();
      Map<Content, List<String>> map = new HashMap<>();

      for (SearchHit hit : hits) {
        Content partialContentWithErrors = new Content();
        Map<String, Object> src = hit.getSourceAsMap();
        partialContentWithErrors.setId((String) src.get("id"));
        partialContentWithErrors.setTitle((String) src.get("title"));
        //partialContentWithErrors.setTags(pair.getKey().getTags()); // TODO: Support tags
        partialContentWithErrors.setPublished((Boolean) src.get("published"));
        partialContentWithErrors.setCanonicalSourceFile((String) src.get("canonicalSourceFile"));

        ArrayList<String> errors = new ArrayList<>();
        for (Object v : (List) hit.getSourceAsMap().get("errors")) {
          errors.add((String) v);
        }

        map.put(partialContentWithErrors, errors);
      }
      return map;

    } catch (SegueSearchException e) {
      log.error("Failed to retrieve problem map from search provider", e);
      return Maps.newHashMap();
    }
  }

  public ContentDTO populateRelatedContent(final ContentDTO contentDTO)
      throws ContentManagerException {
    if (contentDTO.getChildren() != null) {
      for (ContentBaseDTO childBaseContentDTO : contentDTO.getChildren()) {
        if (childBaseContentDTO instanceof ContentDTO) {
          this.populateRelatedContent((ContentDTO) childBaseContentDTO);
        }
      }
    }
    if (contentDTO.getRelatedContent() == null || contentDTO.getRelatedContent().isEmpty()) {
      return contentDTO;
    }

    // build query the db to get full content information
    List<BooleanSearchClause> fieldsToMap = Lists.newArrayList();

    List<String> relatedContentIds = Lists.newArrayList();
    for (ContentSummaryDTO summary : contentDTO.getRelatedContent()) {
      relatedContentIds.add(summary.getId());
    }

    fieldsToMap.add(new BooleanSearchClause(
        Constants.ID_FIELDNAME + '.' + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
        Constants.BooleanOperator.OR, relatedContentIds));

    ResultsWrapper<ContentDTO> results = this.findByFieldNames(fieldsToMap, 0, relatedContentIds.size());

    List<ContentSummaryDTO> relatedContentDTOs = Lists.newArrayList();

    Map<String, ContentDTO> resultsMappedById = Maps.newHashMap();
    for (ContentDTO relatedContent : results.getResults()) {
      resultsMappedById.put(relatedContent.getId(), relatedContent);
    }
    // Iterate over relatedContentIds so that relatedContentDTOs maintain order defined in content not result order
    for (String contentId : relatedContentIds) {
      ContentDTO relatedContent = resultsMappedById.get(contentId);
      if (relatedContent != null) {
        ContentSummaryDTO summary = this.objectMapper.map(relatedContent, ContentSummaryDTO.class);
        GitContentManager.generateDerivedSummaryValues(relatedContent, summary);
        relatedContentDTOs.add(summary);
      } else {
        log.error("Related content with ID '{}' not returned by elasticsearch query", contentId);
      }
    }

    contentDTO.setRelatedContent(relatedContentDTOs);

    return contentDTO;
  }

  public String getCurrentContentSHA() {
    GetResponse shaResponse = contentShaCache.getIfPresent(contentIndex);
    try {
      if (null == shaResponse) {
        shaResponse =
            searchProvider.getById(
                contentIndex,
                ContentIndextype.METADATA.toString(),
                "general"
            );
        contentShaCache.put(contentIndex, shaResponse);
      }
      return (String) shaResponse.getSource().get("version");
    } catch (SegueSearchException e) {
      log.error("Failed to retrieve current content SHA from search provider", e);
      return "unknown";
    }
  }

  /**
   * Returns the basic filter configuration.
   *
   * @return either null or a map setup with filter/exclusion instructions, based on environment properties.
   */
  private Map<String, AbstractFilterInstruction> getBaseFilters() {
    if (!this.hideRegressionTestContent && !this.allowOnlyPublishedContent) {
      return null;
    }

    HashMap<String, AbstractFilterInstruction> filters = new HashMap<>();

    if (this.hideRegressionTestContent) {
      filters.put("tags", new SimpleExclusionInstruction("regression_test"));
    }
    if (this.allowOnlyPublishedContent) {
      filters.put("published", new SimpleFilterInstruction("true"));
    }
    return ImmutableMap.copyOf(filters);
  }

  /**
   * A method which adds information to the contentSummaryDTO, summary, from values evaluated from the content.
   *
   * @param content the original content object which was used to create the summary.
   *                Its instance should not get altered from calling this method.
   * @param summary summary of the content.
   *                The values of this instance could be changed by this method.
   */
  private static void generateDerivedSummaryValues(final ContentDTO content, final ContentSummaryDTO summary) {
    List<String> questionPartIds = Lists.newArrayList();
    GitContentManager.collateQuestionPartIds(content, questionPartIds);
    summary.setQuestionPartIds(questionPartIds);
  }

  /**
   * Recursively walk through the content object and its children to populate the questionPartIds list with the IDs
   * of any content of type QuestionDTO.
   *
   * @param content         the content page and, on recursive invocations, its children.
   * @param questionPartIds a list to track the question part IDs in the content and its children.
   */
  private static void collateQuestionPartIds(final ContentDTO content, final List<String> questionPartIds) {
    if (content instanceof QuestionDTO) {
      questionPartIds.add(content.getId());
    }
    List<ContentBaseDTO> children = content.getChildren();
    if (children != null) {
      for (ContentBaseDTO child : children) {
        if (child instanceof ContentDTO) {
          ContentDTO childContent = (ContentDTO) child;
          collateQuestionPartIds(childContent, questionPartIds);
        }
      }
    }
  }

  public static class BooleanSearchClause {
    private final String field;
    private final Constants.BooleanOperator operator;
    private final List<String> values;

    public BooleanSearchClause(final String field, final Constants.BooleanOperator operator,
                               final List<String> values) {
      this.field = field;
      this.operator = operator;
      this.values = values;
    }

    public String getField() {
      return this.field;
    }

    public Constants.BooleanOperator getOperator() {
      return this.operator;
    }

    public List<String> getValues() {
      return this.values;
    }
  }
}
