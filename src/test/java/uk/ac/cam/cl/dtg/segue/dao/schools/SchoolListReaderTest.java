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

    openSchool = new School("100001", "Open Academy", "AB1 2CD", false, School.SchoolDataSource.GOVERNMENT_UK);
    closedSchool = new School("100002", "Closed Grammar School", "XY9 8ZZ", true, School.SchoolDataSource.GOVERNMENT_UK);

    openSchoolJson = objectMapper.writeValueAsString(openSchool);
    closedSchoolJson = objectMapper.writeValueAsString(closedSchool);

    expect(mockSearchProvider.getById(anyString(), anyString(), anyString()))
        .andThrow(new SegueSearchException("Test - metadata not available")).anyTimes();
    expect(mockSearchProvider.hasIndex(anyString(), anyString()))
        .andReturn(true).anyTimes();
  }

  @Test
  void findSchoolByNameOrPostCode_shouldFilterClosedSchools() throws Exception {
    // Expect fuzzySearch to be called with closed=false filter
    Map<String, List<String>> expectedFilter = ImmutableMap.of(SCHOOL_CLOSED_FIELDNAME, ImmutableList.of("false"));

    expect(mockSearchProvider.fuzzySearch(
        anyObject(BasicSearchParameters.class),
        eq("test query"),
        eq(expectedFilter),
        anyObject(),
        anyString(), anyString(), anyString()
    )).andReturn(new ResultsWrapper<>(ImmutableList.of(openSchoolJson), 1L));

    replay(mockSearchProvider);

    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    List<School> results = reader.findSchoolByNameOrPostCode("test query");

    verify(mockSearchProvider);
    assertEquals(1, results.size());
    assertEquals("100001", results.get(0).getUrn());
    assertEquals("Open Academy", results.get(0).getName());
    assertEquals(false, results.get(0).isClosed());
  }

  @Test
  void findSchoolByNameOrPostCode_shouldReturnEmptyListWhenNoOpenSchoolsMatch() throws Exception {
    // Elasticsearch already filters, so empty results returned
    Map<String, List<String>> expectedFilter = ImmutableMap.of(SCHOOL_CLOSED_FIELDNAME, ImmutableList.of("false"));

    expect(mockSearchProvider.fuzzySearch(
        anyObject(BasicSearchParameters.class),
        eq("closed school query"),
        eq(expectedFilter),
        anyObject(),
        anyString(), anyString(), anyString()
    )).andReturn(new ResultsWrapper<>(Collections.emptyList(), 0L));

    replay(mockSearchProvider);

    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    List<School> results = reader.findSchoolByNameOrPostCode("closed school query");

    verify(mockSearchProvider);
    assertTrue(results.isEmpty());
  }

  @Test
  void findSchoolById_shouldReturnClosedSchool() throws Exception {
    // findByExactMatch should NOT filter by closed status
    expect(mockSearchProvider.findByExactMatch(
        anyObject(BasicSearchParameters.class),
        anyString(),
        eq("100002"),
        anyObject()
    )).andReturn(new ResultsWrapper<>(ImmutableList.of(closedSchoolJson), 1L));

    replay(mockSearchProvider);

    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    School result = reader.findSchoolById("100002");

    verify(mockSearchProvider);
    assertNotNull(result);
    assertEquals("100002", result.getUrn());
    assertEquals("Closed Grammar School", result.getName());
    assertEquals(true, result.isClosed());
  }

  @Test
  void findSchoolById_shouldReturnOpenSchool() throws Exception {
    expect(mockSearchProvider.findByExactMatch(
        anyObject(BasicSearchParameters.class),
        anyString(),
        eq("100001"),
        anyObject()
    )).andReturn(new ResultsWrapper<>(ImmutableList.of(openSchoolJson), 1L));

    replay(mockSearchProvider);

    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    School result = reader.findSchoolById("100001");

    verify(mockSearchProvider);
    assertNotNull(result);
    assertEquals("100001", result.getUrn());
    assertEquals("Open Academy", result.getName());
    assertEquals(false, result.isClosed());
  }

  @Test
  void findSchoolById_shouldReturnNullWhenSchoolNotFound() throws Exception {
    expect(mockSearchProvider.findByExactMatch(
        anyObject(BasicSearchParameters.class),
        anyString(),
        eq("999999"),
        anyObject()
    )).andReturn(new ResultsWrapper<>(Collections.emptyList(), 0L));

    replay(mockSearchProvider);

    SchoolListReader reader = new SchoolListReader(mockSearchProvider);
    School result = reader.findSchoolById("999999");

    verify(mockSearchProvider);
    assertNull(result);
  }
}
