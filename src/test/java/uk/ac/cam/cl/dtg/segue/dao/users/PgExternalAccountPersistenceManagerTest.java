package uk.ac.cam.cl.dtg.segue.dao.users;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.ReflectionUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.*;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;

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

    @Test
    void getRecentlyChangedRecords_WithInvalidUserData_ShouldSkipAndContinue() throws Exception {
      // Arrange
      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      expect(mockPreparedStatement.executeQuery()).andReturn(mockResultSet);

      // First user has invalid data (SQLException)
      expect(mockResultSet.next()).andReturn(true).once();
      expect(mockResultSet.getLong("id")).andThrow(new SQLException("Invalid data"));

      // Second user is valid
      expect(mockResultSet.next()).andReturn(true).once();
      setupMockResultSetForUser(2L, "mailjetId456", "valid@example.com", "TEACHER",
          "Jane", false, "VERIFIED", "[{\"stage\": \"a_level\"}]", false, true, false, false);

      expect(mockResultSet.next()).andReturn(false).once();

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
      assertEquals(2L, result.get(0).getUserId());
    }
  }

  @Nested
  class UpdateProviderLastUpdatedTests {

    @Test
    void updateProviderLastUpdated_WithValidUserId_ShouldUpdateTimestamp() throws Exception {
      // Arrange
      Long userId = 123L;

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setTimestamp(eq(1), anyObject(Timestamp.class));
      expectLastCall();
      mockPreparedStatement.setLong(2, userId);
      expectLastCall();
      expect(mockPreparedStatement.executeUpdate()).andReturn(1);

      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement);

      // Act
      persistenceManager.updateProviderLastUpdated(userId);

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement);
    }

    @Test
    void updateProviderLastUpdated_WithNonExistentUser_ShouldLogWarning() throws Exception {
      // Arrange
      Long userId = 999L;

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setTimestamp(eq(1), anyObject(Timestamp.class));
      expectLastCall();
      mockPreparedStatement.setLong(2, userId);
      expectLastCall();
      expect(mockPreparedStatement.executeUpdate()).andReturn(0); // No rows updated

      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement);

      // Act - Should not throw, just log warning
      persistenceManager.updateProviderLastUpdated(userId);

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement);
    }

    @Test
    void updateProviderLastUpdated_WithNullUserId_ShouldThrowException() {
      // Act & Assert
      assertThrows(IllegalArgumentException.class,
          () -> persistenceManager.updateProviderLastUpdated(null));
    }

    @Test
    void updateProviderLastUpdated_WithDatabaseError_ShouldThrowException() throws Exception {
      // Arrange
      Long userId = 123L;
      expect(mockDatabase.getDatabaseConnection()).andThrow(new SQLException("DB error"));

      replay(mockDatabase);

      // Act & Assert
      assertThrows(SegueDatabaseException.class,
          () -> persistenceManager.updateProviderLastUpdated(userId));

      verify(mockDatabase);
    }
  }

  @Nested
  class UpdateExternalAccountTests {

    @Test
    void updateExternalAccount_WithNewAccount_ShouldInsert() throws Exception {
      // Arrange
      Long userId = 123L;
      String mailjetId = "mailjet456";

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setLong(1, userId);
      expectLastCall();
      mockPreparedStatement.setString(2, mailjetId);
      expectLastCall();
      expect(mockPreparedStatement.executeUpdate()).andReturn(1);

      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement);

      // Act
      persistenceManager.updateExternalAccount(userId, mailjetId);

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement);
    }

    @Test
    void updateExternalAccount_WithExistingAccount_ShouldUpdate() throws Exception {
      // Arrange
      Long userId = 123L;
      String mailjetId = "newMailjetId";

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setLong(1, userId);
      expectLastCall();
      mockPreparedStatement.setString(2, mailjetId);
      expectLastCall();
      expect(mockPreparedStatement.executeUpdate()).andReturn(1);

      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement);

      // Act
      persistenceManager.updateExternalAccount(userId, mailjetId);

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement);
    }

    @Test
    void updateExternalAccount_WithNullMailjetId_ShouldClearAccount() throws Exception {
      // Arrange
      Long userId = 123L;

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setLong(1, userId);
      expectLastCall();
      mockPreparedStatement.setString(2, null);
      expectLastCall();
      expect(mockPreparedStatement.executeUpdate()).andReturn(1);

      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement);

      // Act
      persistenceManager.updateExternalAccount(userId, null);

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement);
    }

    @Test
    void updateExternalAccount_WithNullUserId_ShouldThrowException() {
      // Act & Assert
      assertThrows(IllegalArgumentException.class,
          () -> persistenceManager.updateExternalAccount(null, "mailjetId"));
    }

    @Test
    void updateExternalAccount_WithZeroRowsAffected_ShouldLogWarning() throws Exception {
      // Arrange
      Long userId = 123L;
      String mailjetId = "mailjet456";

      expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection);
      expect(mockConnection.prepareStatement(anyString())).andReturn(mockPreparedStatement);
      mockPreparedStatement.setLong(1, userId);
      expectLastCall();
      mockPreparedStatement.setString(2, mailjetId);
      expectLastCall();
      expect(mockPreparedStatement.executeUpdate()).andReturn(0);

      mockPreparedStatement.close();
      expectLastCall();
      mockConnection.close();
      expectLastCall();

      replay(mockDatabase, mockConnection, mockPreparedStatement);

      // Act - Should not throw, just log warning
      persistenceManager.updateExternalAccount(userId, mailjetId);

      // Assert
      verify(mockDatabase, mockConnection, mockPreparedStatement);
    }

    @Test
    void updateExternalAccount_WithDatabaseError_ShouldThrowException() throws Exception {
      // Arrange
      Long userId = 123L;
      expect(mockDatabase.getDatabaseConnection()).andThrow(new SQLException("DB error"));

      replay(mockDatabase);

      // Act & Assert
      assertThrows(SegueDatabaseException.class,
          () -> persistenceManager.updateExternalAccount(userId, "mailjetId"));

      verify(mockDatabase);
    }
  }

  @Nested
  class StageExtractionTests {

    @ParameterizedTest
    @CsvSource({
        "'[{\"stage\": \"gcse\"}]', GCSE",
        "'[{\"stage\": \"a_level\"}]', A Level",
        "'[{\"stage\": \"a level\"}]', A Level",
        "'[{\"stage\": \"alevel\"}]', A Level",
        "'[{\"stage\": \"gcse_and_a_level\"}]', GCSE and A Level",
        "'[{\"stage\": \"both\"}]', GCSE and A Level",
        "'[{\"stage\": \"all\"}]', ALL"
    })
    void extractStage_WithValidStageValues_ShouldNormalizeCorrectly(String json, String expected) throws Exception {
      // Arrange
      expect(mockResultSet.getLong("id")).andReturn(1L);
      expect(mockResultSet.getString("provider_user_identifier")).andReturn("mailjetId");
      expect(mockResultSet.getString("email")).andReturn("test@example.com");
      expect(mockResultSet.getString("role")).andReturn("STUDENT");
      expect(mockResultSet.getString("given_name")).andReturn("John");
      expect(mockResultSet.getBoolean("deleted")).andReturn(false);
      expect(mockResultSet.getString("email_verification_status")).andReturn("VERIFIED");
      expect(mockResultSet.getBoolean("news_emails")).andReturn(true);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getBoolean("events_emails")).andReturn(false);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getString("registered_contexts")).andReturn(json);

      replay(mockResultSet);

      // Act
      UserExternalAccountChanges result = ReflectionUtils.invokePrivateMethod(
          persistenceManager,
          "buildUserExternalAccountChanges",
          new Class[] {ResultSet.class},
          new Object[] {mockResultSet}
      );

      // Assert
      verify(mockResultSet);
      assertEquals(expected, result.getStage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"[]", "null", "   "})
    void extractStage_WithEmptyOrNullContext_ShouldReturnUnknown(String json) throws Exception {
      // Arrange
      expect(mockResultSet.getLong("id")).andReturn(1L);
      expect(mockResultSet.getString("provider_user_identifier")).andReturn("mailjetId");
      expect(mockResultSet.getString("email")).andReturn("test@example.com");
      expect(mockResultSet.getString("role")).andReturn("STUDENT");
      expect(mockResultSet.getString("given_name")).andReturn("John");
      expect(mockResultSet.getBoolean("deleted")).andReturn(false);
      expect(mockResultSet.getString("email_verification_status")).andReturn("VERIFIED");
      expect(mockResultSet.getBoolean("news_emails")).andReturn(true);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getBoolean("events_emails")).andReturn(false);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getString("registered_contexts")).andReturn(json);

      replay(mockResultSet);

      // Act
      UserExternalAccountChanges result = ReflectionUtils.invokePrivateMethod(
          persistenceManager,
          "buildUserExternalAccountChanges",
          new Class[] {ResultSet.class},
          new Object[] {mockResultSet}
      );

      // Assert
      verify(mockResultSet);
      assertEquals("unknown", result.getStage());
    }

    @Test
    void extractStage_WithInvalidJson_ShouldReturnUnknown() throws Exception {
      // Arrange
      String invalidJson = "[{not valid json}]";

      expect(mockResultSet.getLong("id")).andReturn(1L);
      expect(mockResultSet.getString("provider_user_identifier")).andReturn("mailjetId");
      expect(mockResultSet.getString("email")).andReturn("test@example.com");
      expect(mockResultSet.getString("role")).andReturn("STUDENT");
      expect(mockResultSet.getString("given_name")).andReturn("John");
      expect(mockResultSet.getBoolean("deleted")).andReturn(false);
      expect(mockResultSet.getString("email_verification_status")).andReturn("VERIFIED");
      expect(mockResultSet.getBoolean("news_emails")).andReturn(true);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getBoolean("events_emails")).andReturn(false);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getString("registered_contexts")).andReturn(invalidJson);

      replay(mockResultSet);

      // Act
      UserExternalAccountChanges result = ReflectionUtils.invokePrivateMethod(
          persistenceManager,
          "buildUserExternalAccountChanges",
          new Class[] {ResultSet.class},
          new Object[] {mockResultSet}
      );

      // Assert
      verify(mockResultSet);
      assertEquals("unknown", result.getStage());
    }

    @Test
    void extractStage_WithMissingStageKey_ShouldUseFallbackDetection() throws Exception {
      // Arrange - JSON without explicit 'stage' key but contains stage text
      String jsonWithoutStageKey = "[{\"examBoard\": \"aqa\", \"other\": \"gcse\"}]";

      expect(mockResultSet.getLong("id")).andReturn(1L);
      expect(mockResultSet.getString("provider_user_identifier")).andReturn("mailjetId");
      expect(mockResultSet.getString("email")).andReturn("test@example.com");
      expect(mockResultSet.getString("role")).andReturn("STUDENT");
      expect(mockResultSet.getString("given_name")).andReturn("John");
      expect(mockResultSet.getBoolean("deleted")).andReturn(false);
      expect(mockResultSet.getString("email_verification_status")).andReturn("VERIFIED");
      expect(mockResultSet.getBoolean("news_emails")).andReturn(true);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getBoolean("events_emails")).andReturn(false);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getString("registered_contexts")).andReturn(jsonWithoutStageKey);

      replay(mockResultSet);

      // Act
      UserExternalAccountChanges result = ReflectionUtils.invokePrivateMethod(
          persistenceManager,
          "buildUserExternalAccountChanges",
          new Class[] {ResultSet.class},
          new Object[] {mockResultSet}
      );

      // Assert
      verify(mockResultSet);
      assertEquals("GCSE", result.getStage()); // Fallback should detect "gcse" in the text
    }

    @Test
    void extractStage_WithUnexpectedStageValue_ShouldReturnUnknown() throws Exception {
      // Arrange
      String unexpectedStage = "[{\"stage\": \"university\"}]";

      expect(mockResultSet.getLong("id")).andReturn(1L);
      expect(mockResultSet.getString("provider_user_identifier")).andReturn("mailjetId");
      expect(mockResultSet.getString("email")).andReturn("test@example.com");
      expect(mockResultSet.getString("role")).andReturn("STUDENT");
      expect(mockResultSet.getString("given_name")).andReturn("John");
      expect(mockResultSet.getBoolean("deleted")).andReturn(false);
      expect(mockResultSet.getString("email_verification_status")).andReturn("VERIFIED");
      expect(mockResultSet.getBoolean("news_emails")).andReturn(true);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getBoolean("events_emails")).andReturn(false);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getString("registered_contexts")).andReturn(unexpectedStage);

      replay(mockResultSet);

      // Act
      UserExternalAccountChanges result = ReflectionUtils.invokePrivateMethod(
          persistenceManager,
          "buildUserExternalAccountChanges",
          new Class[] {ResultSet.class},
          new Object[] {mockResultSet}
      );

      // Assert
      verify(mockResultSet);
      assertEquals("unknown", result.getStage());
    }
  }

  @Nested
  class BooleanPreferenceTests {

    @Test
    void parsePreference_WithTrueValue_ShouldReturnTrue() throws Exception {
      // Arrange
      expect(mockResultSet.getLong("id")).andReturn(1L);
      expect(mockResultSet.getString("provider_user_identifier")).andReturn("mailjetId");
      expect(mockResultSet.getString("email")).andReturn("test@example.com");
      expect(mockResultSet.getString("role")).andReturn("STUDENT");
      expect(mockResultSet.getString("given_name")).andReturn("John");
      expect(mockResultSet.getBoolean("deleted")).andReturn(false);
      expect(mockResultSet.getString("email_verification_status")).andReturn("VERIFIED");
      expect(mockResultSet.getBoolean("news_emails")).andReturn(true);
      expect(mockResultSet.wasNull()).andReturn(false); // Not null
      expect(mockResultSet.getBoolean("events_emails")).andReturn(false);
      expect(mockResultSet.wasNull()).andReturn(false);
      expect(mockResultSet.getString("registered_contexts")).andReturn("[]");

      replay(mockResultSet);

      // Act
      UserExternalAccountChanges result = ReflectionUtils.invokePrivateMethod(
          persistenceManager,
          "buildUserExternalAccountChanges",
          new Class[] {ResultSet.class},
          new Object[] {mockResultSet}
      );

      // Assert
      verify(mockResultSet);
      assertTrue(result.allowsNewsEmails());
    }

    @Test
    void parsePreference_WithNullValue_ShouldReturnNull() throws Exception {
      // Arrange
      expect(mockResultSet.getLong("id")).andReturn(1L);
      expect(mockResultSet.getString("provider_user_identifier")).andReturn("mailjetId");
      expect(mockResultSet.getString("email")).andReturn("test@example.com");
      expect(mockResultSet.getString("role")).andReturn("STUDENT");
      expect(mockResultSet.getString("given_name")).andReturn("John");
      expect(mockResultSet.getBoolean("deleted")).andReturn(false);
      expect(mockResultSet.getString("email_verification_status")).andReturn("VERIFIED");
      expect(mockResultSet.getBoolean("news_emails")).andReturn(false);
      expect(mockResultSet.wasNull()).andReturn(true); // Was null
      expect(mockResultSet.getBoolean("events_emails")).andReturn(false);
      expect(mockResultSet.wasNull()).andReturn(true); // Was null

      expect(mockResultSet.getString("registered_contexts")).andReturn("[]");

      replay(mockResultSet);

      // Act
      UserExternalAccountChanges result = ReflectionUtils.invokePrivateMethod(
          persistenceManager,
          "buildUserExternalAccountChanges",
          new Class[] {ResultSet.class},
          new Object[] {mockResultSet}
      );

      // Assert
      verify(mockResultSet);
      assertNull(result.allowsNewsEmails());
      assertNull(result.allowsEventsEmails());
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
