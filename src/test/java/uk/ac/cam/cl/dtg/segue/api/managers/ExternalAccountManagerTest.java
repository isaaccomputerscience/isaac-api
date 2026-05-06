package uk.ac.cam.cl.dtg.segue.api.managers;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mailjet.client.errors.MailjetClientCommunicationException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetRateLimitException;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IExternalAccountDataManager;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper.JobStatus;
import uk.ac.cam.cl.dtg.util.email.MailJetSubscriptionAction;

class ExternalAccountManagerTest {

  private ExternalAccountManager externalAccountManager;
  private IExternalAccountDataManager mockDatabase;
  private MailJetApiClientWrapper mockMailjetApi;

  @BeforeEach
  public void setUp() {
    mockDatabase = createMock(IExternalAccountDataManager.class);
    mockMailjetApi = createMock(MailJetApiClientWrapper.class);
    externalAccountManager = new ExternalAccountManager(mockMailjetApi, mockDatabase);
  }

  @Nested
  class SynchroniseChangedUsersTests {

    @Test
    void synchroniseChangedUsers_WithBulkUsers_ShouldSubmitAndPoll()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange - users with same subscription preferences should be batched
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test1@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, true, "GCSE"
          ),
          new UserExternalAccountChanges(
              2L, null, "test2@example.com", Role.STUDENT, "Jane", false,
              EmailVerificationStatus.VERIFIED, true, true, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), eq(MailJetSubscriptionAction.FORCE_SUBSCRIBE),
          eq(MailJetSubscriptionAction.FORCE_SUBSCRIBE))).andReturn("job123");
      // Job completes successfully with no errors
      expect(mockMailjetApi.getBulkJobStatus("job123"))
          .andReturn(new JobStatus("Completed", 2, 2, 0, 0, 0));
      mockDatabase.batchMarkAsSynced(anyObject(List.class));
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithMixedSubscriptionPreferences_ShouldGroupByPreferences()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange - users with different subscription preferences should be in different groups
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test1@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, true, "GCSE"
          ),
          new UserExternalAccountChanges(
              2L, null, "test2@example.com", Role.STUDENT, "Jane", false,
              EmailVerificationStatus.VERIFIED, true, false, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      // First bulk call for group (FORCE_SUBSCRIBE, FORCE_SUBSCRIBE)
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), eq(MailJetSubscriptionAction.FORCE_SUBSCRIBE),
          eq(MailJetSubscriptionAction.FORCE_SUBSCRIBE))).andReturn("job123");
      expect(mockMailjetApi.getBulkJobStatus("job123"))
          .andReturn(new JobStatus("Completed", 1, 1, 0, 0, 0));

      // Second bulk call for group (FORCE_SUBSCRIBE, UNSUBSCRIBE)
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), eq(MailJetSubscriptionAction.FORCE_SUBSCRIBE),
          eq(MailJetSubscriptionAction.UNSUBSCRIBE))).andReturn("job124");
      expect(mockMailjetApi.getBulkJobStatus("job124"))
          .andReturn(new JobStatus("Completed", 1, 0, 1, 0, 0));

      mockDatabase.batchMarkAsSynced(anyObject(List.class));
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithDeletedUser_ShouldDeleteIndividually()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, "mailjetId123", "test@example.com", Role.STUDENT, "John", true,
              EmailVerificationStatus.VERIFIED, true, true, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      mockMailjetApi.permanentlyDeleteAccountById("mailjetId123");
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, null);
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();
      mockDatabase.batchMarkAsSynced(anyObject(List.class));
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithDeliveryFailedUser_ShouldGroupAsRemove()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.DELIVERY_FAILED, true, true, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      // Delivery failed users should call with REMOVE for both news and events
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), eq(MailJetSubscriptionAction.REMOVE),
          eq(MailJetSubscriptionAction.REMOVE))).andReturn("job123");
      expect(mockMailjetApi.getBulkJobStatus("job123"))
          .andReturn(new JobStatus("Completed", 1, 0, 0, 1, 0));
      mockDatabase.batchMarkAsSynced(anyObject(List.class));
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithEmptyUserList_ShouldReturnWithoutError()
        throws SegueDatabaseException, ExternalAccountSynchronisationException {
      // Arrange
      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(List.of());

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithDatabaseException_ShouldThrow() throws SegueDatabaseException {
      // Arrange
      expect(mockDatabase.getRecentlyChangedRecords())
          .andThrow(new SegueDatabaseException("Database error"));

      replay(mockDatabase);

      // Act & Assert
      assertThrows(ExternalAccountSynchronisationException.class,
          () -> externalAccountManager.synchroniseChangedUsers());

      verify(mockDatabase);
    }

    @Test
    void synchroniseChangedUsers_WithJobErrorsButUserDataCorrect_ShouldMarkAsSynced()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange - job completes but has errors, but user data is correct at Mailjet
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, false, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), anyObject(), anyObject()))
          .andReturn("job123");
      // Job has 1 error
      expect(mockMailjetApi.getBulkJobStatus("job123"))
          .andReturn(new JobStatus("Completed", 1, 0, 0, 0, 1));
      // Recovery: user data is correct in Mailjet despite job error
      JSONObject mailjetContact = new JSONObject()
          .put("Name", "John")
          .put("Properties", new JSONObject()
              .put("role", "STUDENT")
              .put("verification_status", "VERIFIED")
              .put("stage", "GCSE"));
      expect(mockMailjetApi.getAccountByIdOrEmail("test@example.com"))
          .andReturn(mailjetContact);
      mockDatabase.batchMarkAsSynced(anyObject(List.class));
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithJobErrorsAndUserNotFound_ShouldNotSyncUser()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange - job completes with errors and user not found in Mailjet
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, false, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), anyObject(), anyObject()))
          .andReturn("job123");
      // Job has 1 error
      expect(mockMailjetApi.getBulkJobStatus("job123"))
          .andReturn(new JobStatus("Completed", 1, 0, 0, 0, 1));
      // Recovery: user not found in Mailjet after error - treated as failed
      expect(mockMailjetApi.getAccountByIdOrEmail("test@example.com"))
          .andReturn(null);
      // batchMarkAsSynced not called since user failed recovery (empty list)

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithMailjetException_ShouldLogAndContinue()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, false, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), anyObject(), anyObject()))
          .andThrow(new MailjetException("Mailjet error"));

      replay(mockDatabase, mockMailjetApi);

      // Act - Should NOT throw - regular MailjetException is caught and logged
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithCommunicationException_ShouldThrow()
        throws SegueDatabaseException, MailjetException {
      // Arrange
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, false, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), anyObject(), anyObject()))
          .andThrow(new MailjetClientCommunicationException("Communication error"));

      replay(mockDatabase, mockMailjetApi);

      // Act & Assert
      assertThrows(ExternalAccountSynchronisationException.class,
          () -> externalAccountManager.synchroniseChangedUsers());

      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithRateLimitException_ShouldThrow()
        throws SegueDatabaseException, MailjetException {
      // Arrange
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, false, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), anyObject(), anyObject()))
          .andThrow(new MailjetRateLimitException("Rate limit exceeded"));

      replay(mockDatabase, mockMailjetApi);

      // Act & Assert
      ExternalAccountSynchronisationException exception = assertThrows(
          ExternalAccountSynchronisationException.class,
          () -> externalAccountManager.synchroniseChangedUsers());

      assertTrue(exception.getMessage().contains("rate limit"));

      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithSingleRateLimitDuringPolling_ShouldContinuePolling()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange - single rate limit should be tolerated
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, false, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), anyObject(), anyObject()))
          .andReturn("job123");
      // First poll hits rate limit, second succeeds
      expect(mockMailjetApi.getBulkJobStatus("job123"))
          .andThrow(new MailjetRateLimitException("Rate limit"));
      expect(mockMailjetApi.getBulkJobStatus("job123"))
          .andReturn(new JobStatus("Completed", 1, 0, 1, 0, 0));
      mockDatabase.batchMarkAsSynced(anyObject(List.class));
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithRepeatedRateLimitDuringPolling_ShouldFailFast()
        throws SegueDatabaseException, MailjetException {
      // Arrange - 2+ consecutive rate limits should fail fast
      List<UserExternalAccountChanges> changedUsers = List.of(
          new UserExternalAccountChanges(
              1L, null, "test@example.com", Role.STUDENT, "John", false,
              EmailVerificationStatus.VERIFIED, true, false, "GCSE"
          )
      );

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.bulkSyncUsers(anyObject(), anyObject(), anyObject()))
          .andReturn("job123");
      // Both polls hit rate limit
      expect(mockMailjetApi.getBulkJobStatus("job123"))
          .andThrow(new MailjetRateLimitException("Rate limit"))
          .times(2);

      replay(mockDatabase, mockMailjetApi);

      // Act & Assert - should throw on 2nd consecutive rate limit
      ExternalAccountSynchronisationException exception = assertThrows(
          ExternalAccountSynchronisationException.class,
          () -> externalAccountManager.synchroniseChangedUsers());

      assertTrue(exception.getMessage().contains("rate limit"));

      verify(mockDatabase, mockMailjetApi);
    }
  }
}
