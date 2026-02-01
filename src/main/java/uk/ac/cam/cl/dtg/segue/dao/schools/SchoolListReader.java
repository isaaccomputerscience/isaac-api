/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.dao.schools;

import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOLS_INDEX_BASE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_CLOSED_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_NAME_FIELDNAME_POJO;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_POSTCODE_FIELDNAME_POJO;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_URN_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_URN_FIELDNAME_POJO;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SchoolsIndexType;
import static uk.ac.cam.cl.dtg.segue.api.Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX;
import static uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils.getSharedBasicObjectMapper;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import org.elasticsearch.ElasticsearchStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.segue.search.BasicSearchParameters;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

/**
 * Class responsible for reading the local school list csv file.
 * <br>
 * This class is threadsafe providing that the ISearchProvider given as a dependency is not given to another instance of
 * this class. Normally this class should be treated as a singleton to ensure the ISearchProvider is not shared with
 * another instance of this class.
 */
public class SchoolListReader {
  private static final Logger log = LoggerFactory.getLogger(SchoolListReader.class);

  private final ISearchProvider searchProvider;

  private final ObjectMapper mapper = getSharedBasicObjectMapper();

  private final String dataSourceModificationDate;

  /**
   * SchoolListReader constructor.
   *
   * @param searchProvider - search provider that can be used to put and retrieve school data.
   */
  @Inject
  public SchoolListReader(final ISearchProvider searchProvider) {
    log.info("Initializing SchoolListReader");
    this.searchProvider = searchProvider;

    String modificationDate;
    try {
      modificationDate = searchProvider.getById(
              SCHOOLS_INDEX_BASE, SchoolsIndexType.METADATA.toString(), "sourceFile").getSource().get("lastModified")
          .toString();
      log.info("School list data source modification date: {}", modificationDate);
    } catch (SegueSearchException | ElasticsearchStatusException e) {
      log.error("Failed to retrieve school list modification date", e);
      modificationDate = "unknown";
    }
    dataSourceModificationDate = modificationDate;
    log.info("SchoolListReader initialized successfully");
  }

  /**
   * findSchoolByNameOrPostCode.
   *
   * @param searchQuery - school to search for - either name or postcode.
   * @return list of schools matching the criteria or an empty list.
   * @throws UnableToIndexSchoolsException - if there is an error access the index of schools.
   */
  public List<School> findSchoolByNameOrPostCode(final String searchQuery)
      throws UnableToIndexSchoolsException, SegueSearchException {
    log.info("School search initiated with query: {}", sanitiseExternalLogValue(searchQuery));

    if (!this.ensureSchoolList()) {
      log.error("Unable to ensure school search cache.");
      throw new UnableToIndexSchoolsException("unable to ensure the cache has been populated");
    }

    log.info("Executing fuzzy search with closed=false filter");
    List<String> schoolSearchResults = searchProvider.fuzzySearch(
        new BasicSearchParameters(SCHOOLS_INDEX_BASE, SchoolsIndexType.SCHOOL_SEARCH.toString(), 0,
            DEFAULT_RESULTS_LIMIT),
        searchQuery,
        ImmutableMap.of(SCHOOL_CLOSED_FIELDNAME, ImmutableList.of("false")),
        null,
        SCHOOL_URN_FIELDNAME_POJO, SCHOOL_NAME_FIELDNAME_POJO, SCHOOL_POSTCODE_FIELDNAME_POJO
    ).getResults();

    log.info("Elasticsearch returned {} results for query: {}", schoolSearchResults.size(),
        sanitiseExternalLogValue(searchQuery));

    List<School> resultList = Lists.newArrayList();
    int parseErrors = 0;
    for (String schoolString : schoolSearchResults) {
      try {
        resultList.add(mapper.readValue(schoolString, School.class));
      } catch (JsonParseException | JsonMappingException e) {
        log.error("Unable to parse the school {}", schoolString, e);
        parseErrors++;
      } catch (IOException e) {
        log.error("IOException {}", schoolString, e);
        parseErrors++;
      }
    }

    log.info("School search completed. Query: {}, Results: {}, Parse errors: {}",
        sanitiseExternalLogValue(searchQuery), resultList.size(), parseErrors);
    return resultList;
  }

  /**
   * Find school by Id.
   *
   * @param schoolURN - to search for.
   * @return school.
   * @throws UnableToIndexSchoolsException - if we cannot complete the indexing process
   * @throws IOException                   - If we cannot read the school data
   * @throws JsonMappingException          - if we cannot map to the school class.
   * @throws JsonParseException            - if the school data is malformed
   */
  public School findSchoolById(final String schoolURN) throws UnableToIndexSchoolsException, JsonParseException,
      JsonMappingException, IOException, SegueSearchException {
    log.info("School lookup by URN initiated: {}", sanitiseExternalLogValue(schoolURN));

    if (!this.ensureSchoolList()) {
      log.error("Unable to ensure school search cache.");
      throw new UnableToIndexSchoolsException("unable to ensure the cache has been populated");
    }

    log.info("Executing exact match search for URN: {}", sanitiseExternalLogValue(schoolURN));
    List<String> matchingSchoolList;

    matchingSchoolList = searchProvider.findByExactMatch(
        new BasicSearchParameters(SCHOOLS_INDEX_BASE, SchoolsIndexType.SCHOOL_SEARCH.toString(), 0,
            DEFAULT_RESULTS_LIMIT),
        SCHOOL_URN_FIELDNAME.toLowerCase() + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, schoolURN, null).getResults();

    if (matchingSchoolList.isEmpty()) {
      log.info("School lookup completed. URN: {} not found in index", sanitiseExternalLogValue(schoolURN));
      return null;
    }

    if (matchingSchoolList.size() > 1) {
      log.error("Error occurred while trying to look a school up by id... Found more than one match for "
          + sanitiseExternalLogValue(schoolURN) + " results: " + matchingSchoolList);
    }

    School school = mapper.readValue(matchingSchoolList.get(0), School.class);
    log.info("School lookup completed. URN: {}, Found: {}, Closed: {}",
        sanitiseExternalLogValue(schoolURN), school.getName(), school.isClosed());
    return school;
  }


  /**
   * Ensure School List has been generated.
   *
   * @return true if we have an index or false if not. If false we cannot guarantee a response.
   */
  private boolean ensureSchoolList() {
    boolean indexExists = searchProvider.hasIndex(SCHOOLS_INDEX_BASE, SchoolsIndexType.SCHOOL_SEARCH.toString());
    if (!indexExists) {
      log.warn("School index not found: {}/{}", SCHOOLS_INDEX_BASE, SchoolsIndexType.SCHOOL_SEARCH);
    }
    return indexExists;
  }


  /**
   * Method to help determine freshness of data.
   *
   * @return date when the data source was last modified.
   */
  public String getDataLastModifiedDate() {
    return this.dataSourceModificationDate;
  }
}
