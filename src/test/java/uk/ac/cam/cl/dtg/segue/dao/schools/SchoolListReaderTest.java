package uk.ac.cam.cl.dtg.segue.dao.schools;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.get.GetResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.search.BasicSearchParameters;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

class SchoolListReaderTest {

  private SchoolListReader schoolListReader;
  private ISearchProvider mockSearchProvider;
  private PgSchoolLookup mockFallbackLookup;
  private GetResponse mockGetResponse;

  @BeforeEach
  void setUp() throws SegueSearchException {
    mockSearchProvider = createMock(ISearchProvider.class);
    mockFallbackLookup = createMock(PgSchoolLookup.class);
    mockGetResponse = createNiceMock(GetResponse.class);

    // Setup default behavior for index metadata lookup (called in constructor)
    expect(mockGetResponse.getSource()).andReturn(Map.of("lastModified", "1234567890")).anyTimes();
    replay(mockGetResponse);

    expect(mockSearchProvider.getById(anyString(), anyString(), anyString()))
        .andReturn(mockGetResponse).anyTimes();
    expect(mockSearchProvider.hasIndex(anyString(), anyString())).andReturn(true).anyTimes();
  }

  @AfterEach
  void tearDown() {
    reset(mockSearchProvider, mockFallbackLookup, mockGetResponse);
  }

  @Nested
  class FindSchoolByIdWithFallbackTests {

    @Test
    void findSchoolById_WhenFoundInElasticsearch_ShouldReturnSchool() throws Exception {
      // Arrange
      String urn = "102354";
      String schoolJson = "{\"urn\":\"102354\",\"name\":\"Test School\",\"postcode\":\"AB1 2CD\",\"closed\":false}";

      ResultsWrapper<String> resultsWrapper = new ResultsWrapper<>(List.of(schoolJson), 1L);
      expect(mockSearchProvider.findByExactMatch(anyObject(BasicSearchParameters.class), anyString(), anyString(),
          anyObject()))
          .andReturn(resultsWrapper);

      replay(mockSearchProvider, mockFallbackLookup);

      schoolListReader = new SchoolListReader(mockSearchProvider, mockFallbackLookup);

      // Act
      School result = schoolListReader.findSchoolById(urn);

      // Assert
      verify(mockSearchProvider);
      assertNotNull(result);
      assertEquals(urn, result.getUrn());
      assertEquals("Test School", result.getName());
    }

    @Test
    void findSchoolById_WhenNotInElasticsearchButInFallback_ShouldReturnFromFallback() throws Exception {
      // Arrange
      String urn = "102354";
      School fallbackSchool = new School();
      fallbackSchool.setUrn(urn);
      fallbackSchool.setName("Fallback School");
      fallbackSchool.setClosed(true);

      ResultsWrapper<String> emptyResults = new ResultsWrapper<>(Collections.emptyList(), 0L);
      expect(mockSearchProvider.findByExactMatch(anyObject(BasicSearchParameters.class), anyString(), anyString(),
          anyObject()))
          .andReturn(emptyResults);

      expect(mockFallbackLookup.findSchoolById(urn)).andReturn(fallbackSchool);

      replay(mockSearchProvider, mockFallbackLookup);

      schoolListReader = new SchoolListReader(mockSearchProvider, mockFallbackLookup);

      // Act
      School result = schoolListReader.findSchoolById(urn);

      // Assert
      verify(mockSearchProvider, mockFallbackLookup);
      assertNotNull(result);
      assertEquals(urn, result.getUrn());
      assertEquals("Fallback School", result.getName());
      assertTrue(result.isClosed());
    }

    @Test
    void findSchoolById_WhenNotInElasticsearchAndNotInFallback_ShouldReturnNull() throws Exception {
      // Arrange
      String urn = "999999";

      ResultsWrapper<String> emptyResults = new ResultsWrapper<>(Collections.emptyList(), 0L);
      expect(mockSearchProvider.findByExactMatch(anyObject(BasicSearchParameters.class), anyString(), anyString(),
          anyObject()))
          .andReturn(emptyResults);

      expect(mockFallbackLookup.findSchoolById(urn)).andReturn(null);

      replay(mockSearchProvider, mockFallbackLookup);

      schoolListReader = new SchoolListReader(mockSearchProvider, mockFallbackLookup);

      // Act
      School result = schoolListReader.findSchoolById(urn);

      // Assert
      verify(mockSearchProvider, mockFallbackLookup);
      assertNull(result);
    }

    @Test
    void findSchoolById_WhenFallbackThrowsException_ShouldReturnNull() throws Exception {
      // Arrange
      String urn = "102354";

      ResultsWrapper<String> emptyResults = new ResultsWrapper<>(Collections.emptyList(), 0L);
      expect(mockSearchProvider.findByExactMatch(anyObject(BasicSearchParameters.class), anyString(), anyString(),
          anyObject()))
          .andReturn(emptyResults);

      expect(mockFallbackLookup.findSchoolById(urn)).andThrow(new SegueDatabaseException("DB error"));

      replay(mockSearchProvider, mockFallbackLookup);

      schoolListReader = new SchoolListReader(mockSearchProvider, mockFallbackLookup);

      // Act
      School result = schoolListReader.findSchoolById(urn);

      // Assert
      verify(mockSearchProvider, mockFallbackLookup);
      assertNull(result);
    }

    @Test
    void findSchoolById_WhenNoFallbackConfigured_ShouldReturnNull() throws Exception {
      // Arrange
      String urn = "102354";

      ResultsWrapper<String> emptyResults = new ResultsWrapper<>(Collections.emptyList(), 0L);
      expect(mockSearchProvider.findByExactMatch(anyObject(BasicSearchParameters.class), anyString(), anyString(),
          anyObject()))
          .andReturn(emptyResults);

      replay(mockSearchProvider);

      // Create reader without fallback
      schoolListReader = new SchoolListReader(mockSearchProvider);

      // Act
      School result = schoolListReader.findSchoolById(urn);

      // Assert
      verify(mockSearchProvider);
      assertNull(result);
    }
  }
}
