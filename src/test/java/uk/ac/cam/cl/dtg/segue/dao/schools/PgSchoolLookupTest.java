package uk.ac.cam.cl.dtg.segue.dao.schools;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

class PgSchoolLookupTest {

  private PgSchoolLookup pgSchoolLookup;
  private PostgresSqlDb mockDatabase;
  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;

  @BeforeEach
  void setUp() {
    mockDatabase = createMock(PostgresSqlDb.class);
    mockConnection = createMock(Connection.class);
    mockPreparedStatement = createMock(PreparedStatement.class);
    mockResultSet = createMock(ResultSet.class);
    pgSchoolLookup = new PgSchoolLookup(mockDatabase);
  }

  @AfterEach
  void tearDown() {
    reset(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);
  }

  @Nested
  class FindSchoolByIdTests {

    @Test
    void findSchoolById_WithValidUrn_ShouldReturnSchool() throws Exception {
      // Arrange
      String urn = "102354";
      String schoolName = "Sacred Heart of Mary Girls' School";

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setString(1, urn);
      expectLastCall();
      expect(mockPreparedStatement.executeQuery()).andReturn(mockResultSet);

      expect(mockResultSet.next()).andReturn(true);
      expect(mockResultSet.getString("urn")).andReturn(urn);
      expect(mockResultSet.getString("school_name")).andReturn(schoolName);

      mockResultSet.close();
      expectLastCall();
      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);

      // Act
      School result = pgSchoolLookup.findSchoolById(urn);

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);
      assertNotNull(result);
      assertEquals(urn, result.getUrn());
      assertEquals(schoolName, result.getName());
      assertTrue(result.isClosed());
      assertEquals(School.SchoolDataSource.GOVERNMENT_UK, result.getDataSource());
    }

    @Test
    void findSchoolById_WithUnknownUrn_ShouldReturnNull() throws Exception {
      // Arrange
      String urn = "999999";

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setString(1, urn);
      expectLastCall();
      expect(mockPreparedStatement.executeQuery()).andReturn(mockResultSet);

      expect(mockResultSet.next()).andReturn(false);

      mockResultSet.close();
      expectLastCall();
      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);

      // Act
      School result = pgSchoolLookup.findSchoolById(urn);

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);
      assertNull(result);
    }

    @Test
    void findSchoolById_WithNullUrn_ShouldReturnNull() throws Exception {
      // Arrange - no database interaction expected

      replay(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);

      // Act
      School result = pgSchoolLookup.findSchoolById(null);

      // Assert
      assertNull(result);
    }

    @Test
    void findSchoolById_WithEmptyUrn_ShouldReturnNull() throws Exception {
      // Arrange - no database interaction expected

      replay(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);

      // Act
      School result = pgSchoolLookup.findSchoolById("");

      // Assert
      assertNull(result);
    }

    @Test
    void findSchoolById_WithDatabaseError_ShouldThrowException() throws Exception {
      // Arrange
      String urn = "102354";

      expect(mockDatabase.getDatabaseConnection()).andThrow(new SQLException("Connection failed"));

      replay(mockDatabase);

      // Act & Assert
      assertThrows(SegueDatabaseException.class,
          () -> pgSchoolLookup.findSchoolById(urn));

      verify(mockDatabase);
    }

    @Test
    void findSchoolById_WithQueryError_ShouldThrowException() throws Exception {
      // Arrange
      String urn = "102354";

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setString(1, urn);
      expectLastCall();
      expect(mockPreparedStatement.executeQuery()).andThrow(new SQLException("Query failed"));

      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);

      // Act & Assert
      assertThrows(SegueDatabaseException.class,
          () -> pgSchoolLookup.findSchoolById(urn));

      verify(mockDatabase, mockConnection, mockPreparedStatement);
    }
  }
}
