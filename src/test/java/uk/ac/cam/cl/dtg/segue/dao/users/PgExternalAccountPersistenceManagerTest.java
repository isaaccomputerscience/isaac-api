package uk.ac.cam.cl.dtg.segue.dao.users;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

class PgExternalAccountPersistenceManagerTest {

  private PgExternalAccountPersistenceManager persistenceManager;
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
    persistenceManager = new PgExternalAccountPersistenceManager(mockDatabase);
  }

  @AfterEach
  void tearDown() {
    reset(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);
  }

  @Nested
  class GetRecentlyChangedRecordsTests {

    @Test
    void getRecentlyChangedRecords_WithValidData_ShouldReturnUserList() throws Exception {
      // Arrange
      String registeredContextsJson = "[{\"stage\": \"gcse\"}]";

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      expect(mockPreparedStatement.executeQuery()).andReturn(mockResultSet);

      expect(mockResultSet.next()).andReturn(true).once();
      expect(mockResultSet.next()).andReturn(false).once();

      setupMockResultSetForUser(1L, "mailjetId123", "test@example.com", "STUDENT",
          "John", false, "VERIFIED", registeredContextsJson, true, false, false, false);

      mockResultSet.close();
      expectLastCall();
      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);

      // Act
      List<UserExternalAccountChanges> result = persistenceManager.getRecentlyChangedRecords();

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);
      assertEquals(1, result.size());
      UserExternalAccountChanges user = result.get(0);
      assertEquals(1L, user.getUserId());
      assertEquals("mailjetId123", user.getProviderUserId());
      assertEquals("test@example.com", user.getAccountEmail());
      assertEquals("GCSE", user.getStage());
    }

    @Test
    void getRecentlyChangedRecords_WithEmptyResults_ShouldReturnEmptyList() throws Exception {
      // Arrange
      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
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
      List<UserExternalAccountChanges> result = persistenceManager.getRecentlyChangedRecords();

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement, mockResultSet);
      assertTrue(result.isEmpty());
    }

    @Test
    void getRecentlyChangedRecords_WithDatabaseError_ShouldThrowException() throws Exception {
      // Arrange
      expect(mockDatabase.getDatabaseConnection()).andThrow(new SQLException("Connection failed"));

      replay(mockDatabase);

      // Act & Assert
      assertThrows(SegueDatabaseException.class,
          () -> persistenceManager.getRecentlyChangedRecords());

      verify(mockDatabase);
    }
  }

  // Helper method to setup mock ResultSet with all expected calls
  private void setupMockResultSetForUser(Long userId, String mailjetId, String email, String role,
                                         String givenName, boolean deleted, String verificationStatus,
                                         String registeredContexts, boolean newsEmails, boolean eventsEmails,
                                         boolean newsWasNull, boolean eventsWasNull) throws SQLException {
    expect(mockResultSet.getLong("id")).andReturn(userId);
    expect(mockResultSet.getString("provider_user_identifier")).andReturn(mailjetId);
    expect(mockResultSet.getString("email")).andReturn(email);
    expect(mockResultSet.getString("role")).andReturn(role);
    expect(mockResultSet.getString("given_name")).andReturn(givenName);
    expect(mockResultSet.getBoolean("deleted")).andReturn(deleted);
    expect(mockResultSet.getString("email_verification_status")).andReturn(verificationStatus);
    expect(mockResultSet.getBoolean("news_emails")).andReturn(newsEmails);
    expect(mockResultSet.wasNull()).andReturn(newsWasNull);
    expect(mockResultSet.getBoolean("events_emails")).andReturn(eventsEmails);
    expect(mockResultSet.wasNull()).andReturn(eventsWasNull);
    expect(mockResultSet.getString("registered_contexts")).andReturn(registeredContexts);
  }

}
