/**
 * Copyright 2025 Raspberry Pi Foundation
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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SCHOOL_CLOSED_FIELDNAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.search.BasicSearchParameters;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

/**
 * Tests for SchoolListReader.
 * <br>
 * Verifies that:
 * - findSchoolByNameOrPostCode filters out closed schools at the Elasticsearch query level
 * - findSchoolById returns schools regardless of closed status (for existing user profiles)
 */
class SchoolListReaderTest {

  private ISearchProvider mockSearchProvider;
  private ObjectMapper objectMapper;

  private School openSchool;
  private School closedSchool;
  private String openSchoolJson;
  private String closedSchoolJson;

  @BeforeEach
  void setUp() throws JsonProcessingException, SegueSearchException {
    mockSearchProvider = createNiceMock(ISearchProvider.class);
    objectMapper = new ObjectMapper();

    // Create test schools
    openSchool = new School("100001", "Open Academy", "AB1 2CD", false, School.SchoolDataSource.GOVERNMENT_UK);
    closedSchool = new School("100002", "Closed Grammar School", "XY9 8ZZ", true, School.SchoolDataSource.GOVERNMENT_UK);

    openSchoolJson = objectMapper.writeValueAsString(openSchool);
    closedSchoolJson = objectMapper.writeValueAsString(closedSchool);

    // Constructor calls getById - make it throw an exception which is caught and handled
    // (sets dataSourceModificationDate to "unknown")
    expect(mockSearchProvider.getById(anyString(), anyString(), anyString()))
        .andThrow(new SegueSearchException("Test - metadata not available")).anyTimes();
    expect(mockSearchProvider.hasIndex(anyString(), anyString()))
        .andReturn(true).anyTimes();
  }

  @Test
  void findSchoolByNameOrPostCode_shouldFilterClosedSchools() throws Exception {
    // Arrange - expect fuzzySearch to be called with closed=false filter
    Map<String, List<String>> expectedFilter = ImmutableMap.of(SCHOOL_CLOSED_FIELDNAME, ImmutableList.of("false"));

    expect(mockSearchProvider.fuzzySearch(
        anyObject(BasicSearchParameters.class),
        eq("test query"),
        eq(expectedFilter),
        anyObject(),
        anyString(), anyString(), anyString()
    )).andReturn(new ResultsWrapper<>(ImmutableList.of(openSchoolJson), 1L));

    replay(mockSearchProvider);

    // Act
    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    List<School> results = reader.findSchoolByNameOrPostCode("test query");

    // Assert
    verify(mockSearchProvider);
    assertEquals(1, results.size());
    assertEquals("100001", results.get(0).getUrn());
    assertEquals("Open Academy", results.get(0).getName());
    assertEquals(false, results.get(0).isClosed());
  }

  @Test
  void findSchoolByNameOrPostCode_shouldReturnEmptyListWhenNoOpenSchoolsMatch() throws Exception {
    // Arrange - Elasticsearch already filters, so empty results returned
    Map<String, List<String>> expectedFilter = ImmutableMap.of(SCHOOL_CLOSED_FIELDNAME, ImmutableList.of("false"));

    expect(mockSearchProvider.fuzzySearch(
        anyObject(BasicSearchParameters.class),
        eq("closed school query"),
        eq(expectedFilter),
        anyObject(),
        anyString(), anyString(), anyString()
    )).andReturn(new ResultsWrapper<>(Collections.emptyList(), 0L));

    replay(mockSearchProvider);

    // Act
    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    List<School> results = reader.findSchoolByNameOrPostCode("closed school query");

    // Assert
    verify(mockSearchProvider);
    assertTrue(results.isEmpty());
  }

  @Test
  void findSchoolById_shouldReturnClosedSchool() throws Exception {
    // Arrange - findByExactMatch should NOT filter by closed status
    expect(mockSearchProvider.findByExactMatch(
        anyObject(BasicSearchParameters.class),
        anyString(),
        eq("100002"),
        anyObject()
    )).andReturn(new ResultsWrapper<>(ImmutableList.of(closedSchoolJson), 1L));

    replay(mockSearchProvider);

    // Act
    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    School result = reader.findSchoolById("100002");

    // Assert
    verify(mockSearchProvider);
    assertNotNull(result);
    assertEquals("100002", result.getUrn());
    assertEquals("Closed Grammar School", result.getName());
    assertEquals(true, result.isClosed());
  }

  @Test
  void findSchoolById_shouldReturnOpenSchool() throws Exception {
    // Arrange
    expect(mockSearchProvider.findByExactMatch(
        anyObject(BasicSearchParameters.class),
        anyString(),
        eq("100001"),
        anyObject()
    )).andReturn(new ResultsWrapper<>(ImmutableList.of(openSchoolJson), 1L));

    replay(mockSearchProvider);

    // Act
    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    School result = reader.findSchoolById("100001");

    // Assert
    verify(mockSearchProvider);
    assertNotNull(result);
    assertEquals("100001", result.getUrn());
    assertEquals("Open Academy", result.getName());
    assertEquals(false, result.isClosed());
  }

  @Test
  void findSchoolById_shouldReturnNullWhenSchoolNotFound() throws Exception {
    // Arrange
    expect(mockSearchProvider.findByExactMatch(
        anyObject(BasicSearchParameters.class),
        anyString(),
        eq("999999"),
        anyObject()
    )).andReturn(new ResultsWrapper<>(Collections.emptyList(), 0L));

    replay(mockSearchProvider);

    // Act
    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    School result = reader.findSchoolById("999999");

    // Assert
    verify(mockSearchProvider);
    assertNull(result);
  }
}
