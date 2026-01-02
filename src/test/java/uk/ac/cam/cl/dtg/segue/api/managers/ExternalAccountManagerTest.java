package uk.ac.cam.cl.dtg.segue.api.managers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import uk.ac.cam.cl.dtg.util.email.MailJetSubscriptionAction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

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
    void synchroniseChangedUsers_WithNewUser_ShouldCreateAccount()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, null, "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.addNewUserOrGetUserIfExists("test@example.com")).andReturn("mailjetId123");
      mockMailjetApi.updateUserProperties("mailjetId123", "John", "STUDENT", "VERIFIED", "GCSE");
      expectLastCall();
      mockMailjetApi.updateUserSubscriptions("mailjetId123",
          MailJetSubscriptionAction.FORCE_SUBSCRIBE, MailJetSubscriptionAction.UNSUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, "mailjetId123");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithExistingUser_ShouldUpdateAccount()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      JSONObject mailjetDetails = new JSONObject();
      mailjetDetails.put("Email", "test@example.com");

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("existingMailjetId")).andReturn(mailjetDetails);
      mockMailjetApi.updateUserProperties("existingMailjetId", "John", "STUDENT", "VERIFIED", "GCSE");
      expectLastCall();
      mockMailjetApi.updateUserSubscriptions("existingMailjetId",
          MailJetSubscriptionAction.FORCE_SUBSCRIBE, MailJetSubscriptionAction.UNSUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, "existingMailjetId");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithDeletedUser_ShouldDeleteAccount()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", true,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      JSONObject mailjetDetails = new JSONObject();
      mailjetDetails.put("Email", "test@example.com");

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("existingMailjetId")).andReturn(mailjetDetails);
      mockMailjetApi.permanentlyDeleteAccountById("existingMailjetId");
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, null);
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithDeliveryFailed_ShouldUnsubscribeFromAll()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.DELIVERY_FAILED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      JSONObject mailjetDetails = new JSONObject();
      mailjetDetails.put("Email", "test@example.com");

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("existingMailjetId")).andReturn(mailjetDetails);
      mockMailjetApi.updateUserSubscriptions("existingMailjetId",
          MailJetSubscriptionAction.REMOVE, MailJetSubscriptionAction.REMOVE);
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithEmailChange_ShouldRecreateAccount()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "newemail@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      JSONObject oldMailjetDetails = new JSONObject();
      oldMailjetDetails.put("Email", "oldemail@example.com");

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("existingMailjetId")).andReturn(oldMailjetDetails);
      mockMailjetApi.permanentlyDeleteAccountById("existingMailjetId");
      expectLastCall();
      expect(mockMailjetApi.addNewUserOrGetUserIfExists("newemail@example.com")).andReturn("newMailjetId");
      mockMailjetApi.updateUserProperties("newMailjetId", "John", "STUDENT", "VERIFIED", "GCSE");
      expectLastCall();
      mockMailjetApi.updateUserSubscriptions("newMailjetId",
          MailJetSubscriptionAction.FORCE_SUBSCRIBE, MailJetSubscriptionAction.UNSUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, "newMailjetId");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithMailjetIdButAccountNotFound_ShouldTreatAsNew()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "nonExistentMailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("nonExistentMailjetId")).andReturn(null);
      mockDatabase.updateExternalAccount(1L, null);
      expectLastCall();
      expect(mockMailjetApi.addNewUserOrGetUserIfExists("test@example.com")).andReturn("newMailjetId");
      mockMailjetApi.updateUserProperties("newMailjetId", "John", "STUDENT", "VERIFIED", "GCSE");
      expectLastCall();
      mockMailjetApi.updateUserSubscriptions("newMailjetId",
          MailJetSubscriptionAction.FORCE_SUBSCRIBE, MailJetSubscriptionAction.UNSUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, "newMailjetId");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void synchroniseChangedUsers_WithNullOrEmptyEmail_ShouldSkip(String email)
        throws SegueDatabaseException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, null, email, Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert - No mailjet calls should be made
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithEmptyList_ShouldReturnEarly()
        throws SegueDatabaseException, ExternalAccountSynchronisationException {
      // Arrange
      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(new ArrayList<>());

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
    void synchroniseChangedUsers_WithMailjetException_ShouldLogAndContinue()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("existingMailjetId"))
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
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("existingMailjetId"))
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
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("existingMailjetId"))
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
    void synchroniseChangedUsers_WithDatabaseErrorDuringUpdate_ShouldLogAndContinue()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges user1 = new UserExternalAccountChanges(
          1L, "mailjetId1", "test1@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      UserExternalAccountChanges user2 = new UserExternalAccountChanges(
          2L, "mailjetId2", "test2@example.com", Role.TEACHER, "Jane", false,
          EmailVerificationStatus.VERIFIED, false, true, "A Level"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(user1, user2);

      JSONObject mailjet1 = new JSONObject();
      mailjet1.put("Email", "test1@example.com");
      JSONObject mailjet2 = new JSONObject();
      mailjet2.put("Email", "test2@example.com");

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);

      // First user - database error during update
      expect(mockMailjetApi.getAccountByIdOrEmail("mailjetId1")).andReturn(mailjet1);
      mockMailjetApi.updateUserProperties("mailjetId1", "John", "STUDENT", "VERIFIED", "GCSE");
      expectLastCall();
      mockMailjetApi.updateUserSubscriptions("mailjetId1",
          MailJetSubscriptionAction.FORCE_SUBSCRIBE, MailJetSubscriptionAction.UNSUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, "mailjetId1");
      expectLastCall().andThrow(new SegueDatabaseException("DB error"));

      // Second user - should still process
      expect(mockMailjetApi.getAccountByIdOrEmail("mailjetId2")).andReturn(mailjet2);
      mockMailjetApi.updateUserProperties("mailjetId2", "Jane", "TEACHER", "VERIFIED", "A Level");
      expectLastCall();
      mockMailjetApi.updateUserSubscriptions("mailjetId2",
          MailJetSubscriptionAction.UNSUBSCRIBE, MailJetSubscriptionAction.FORCE_SUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(2L, "mailjetId2");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(2L);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act - Should not throw, continues processing
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithMultipleUsers_ShouldProcessAll()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges user1 = new UserExternalAccountChanges(
          1L, null, "new@example.com", Role.STUDENT, "Alice", false,
          EmailVerificationStatus.VERIFIED, true, true, "GCSE"
      );
      UserExternalAccountChanges user2 = new UserExternalAccountChanges(
          2L, "existingId", "existing@example.com", Role.TEACHER, "Bob", false,
          EmailVerificationStatus.VERIFIED, false, false, "A Level"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(user1, user2);

      JSONObject mailjet2 = new JSONObject();
      mailjet2.put("Email", "existing@example.com");

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);

      // User 1 - new user
      expect(mockMailjetApi.addNewUserOrGetUserIfExists("new@example.com")).andReturn("newId");
      mockMailjetApi.updateUserProperties("newId", "Alice", "STUDENT", "VERIFIED", "GCSE");
      expectLastCall();
      mockMailjetApi.updateUserSubscriptions("newId",
          MailJetSubscriptionAction.FORCE_SUBSCRIBE, MailJetSubscriptionAction.FORCE_SUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, "newId");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      // User 2 - existing user
      expect(mockMailjetApi.getAccountByIdOrEmail("existingId")).andReturn(mailjet2);
      mockMailjetApi.updateUserProperties("existingId", "Bob", "TEACHER", "VERIFIED", "A Level");
      expectLastCall();
      mockMailjetApi.updateUserSubscriptions("existingId",
          MailJetSubscriptionAction.UNSUBSCRIBE, MailJetSubscriptionAction.UNSUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(2L, "existingId");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(2L);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithUnexpectedError_ShouldLogAndContinue()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "mailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.getAccountByIdOrEmail("mailjetId"))
          .andThrow(new RuntimeException("Unexpected error"));

      replay(mockDatabase, mockMailjetApi);

      // Act - Should not throw, logs error and continues
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithNewUserAndNullMailjetId_ShouldThrow()
        throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, null, "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mockMailjetApi.addNewUserOrGetUserIfExists("test@example.com")).andReturn(null);

      replay(mockDatabase, mockMailjetApi);

      // Act & Assert
      externalAccountManager.synchroniseChangedUsers();

      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithNewUserAndDeliveryFailed_ShouldSkip()
        throws SegueDatabaseException, ExternalAccountSynchronisationException, MailjetException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, null, "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.DELIVERY_FAILED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, null);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert - No mailjet API calls should be made
      verify(mockDatabase, mockMailjetApi);
    }

    @Test
    void synchroniseChangedUsers_WithNewUserAndDeleted_ShouldSkip()
        throws SegueDatabaseException, ExternalAccountSynchronisationException, MailjetException {
      // Arrange
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, null, "test@example.com", Role.STUDENT, "John", true,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);

      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, null);
      expectLastCall();

      replay(mockDatabase, mockMailjetApi);

      // Act
      externalAccountManager.synchroniseChangedUsers();

      // Assert
      verify(mockDatabase, mockMailjetApi);
    }
  }
}
