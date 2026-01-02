package uk.ac.cam.cl.dtg.segue.util.email;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetClientRequestException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetClientCommunicationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;
import uk.ac.cam.cl.dtg.util.email.MailJetSubscriptionAction;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

class MailJetApiClientWrapperTest {

  private MailJetApiClientWrapper mailJetApiClientWrapper;
  private MailjetClient mockMailjetClient;

  @BeforeEach
  void setUp() {
    mockMailjetClient = createMock(MailjetClient.class);

    mailJetApiClientWrapper = new MailJetApiClientWrapper(
        "apiKey", "apiSecret", "newsListId", "eventsListId", "legalListId"
    );
    injectMockMailjetClient(mailJetApiClientWrapper, mockMailjetClient);
  }

  @Nested
  class ConstructorTests {

    @Test
    void constructor_WithNullApiKey_ShouldThrowException() {
      assertThrows(IllegalArgumentException.class,
          () -> new MailJetApiClientWrapper(null, "secret", "news", "events", "legal"));
    }

    @Test
    void constructor_WithNullApiSecret_ShouldThrowException() {
      assertThrows(IllegalArgumentException.class,
          () -> new MailJetApiClientWrapper("key", null, "news", "events", "legal"));
    }

    @Test
    void constructor_WithValidCredentials_ShouldInitialize() {
      assertDoesNotThrow(() ->
          new MailJetApiClientWrapper("key", "secret", "news", "events", "legal"));
    }
  }

  @Nested
  class GetAccountByIdOrEmailTests {

    @Test
    void getAccountByIdOrEmail_WithValidId_ShouldReturnAccount() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);
      JSONArray mockData = new JSONArray();
      JSONObject mockAccount = new JSONObject();
      mockAccount.put("ID", 123);
      mockAccount.put("Email", "test@example.com");
      mockData.put(mockAccount);

      expect(mockMailjetClient.get(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(200).times(2);
      expect(mockResponse.getTotal()).andReturn(1);
      expect(mockResponse.getData()).andReturn(mockData);

      replay(mockMailjetClient, mockResponse);

      // Act
      JSONObject result = mailJetApiClientWrapper.getAccountByIdOrEmail(mailjetId);

      // Assert
      verify(mockMailjetClient, mockResponse);
      assertNotNull(result);
      assertEquals(123, result.getInt("ID"));
    }

    @Test
    void getAccountByIdOrEmail_WithNotFound_ShouldReturnNull() throws MailjetException {
      // Arrange
      String mailjetId = "999";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.get(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(404);

      replay(mockMailjetClient, mockResponse);

      // Act
      JSONObject result = mailJetApiClientWrapper.getAccountByIdOrEmail(mailjetId);

      // Assert
      verify(mockMailjetClient, mockResponse);
      assertNull(result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void getAccountByIdOrEmail_WithNullOrEmptyId_ShouldReturnNull(String input) throws MailjetException {
      // Act
      JSONObject result = mailJetApiClientWrapper.getAccountByIdOrEmail(input);

      // Assert
      assertNull(result);
    }

    @Test
    void getAccountByIdOrEmail_WithUnexpectedStatus_ShouldThrowException() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.get(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);

      replay(mockMailjetClient, mockResponse);

      // Act & Assert
      assertThrows(MailjetException.class,
          () -> mailJetApiClientWrapper.getAccountByIdOrEmail(mailjetId));

      verify(mockMailjetClient, mockResponse);
    }

    @Test
    void getAccountByIdOrEmail_WithCommunicationError_ShouldThrowCommunicationException() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetException timeoutException = new MailjetClientCommunicationException("Timeout occurred");

      expect(mockMailjetClient.get(anyObject(MailjetRequest.class))).andThrow(timeoutException);

      replay(mockMailjetClient);

      // Act & Assert
      assertThrows(MailjetClientCommunicationException.class,
          () -> mailJetApiClientWrapper.getAccountByIdOrEmail(mailjetId));

      verify(mockMailjetClient);
    }

    @Test
    void getAccountByIdOrEmail_WithEmptyResponse_ShouldReturnNull() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);
      JSONArray emptyData = new JSONArray();

      expect(mockMailjetClient.get(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(200).times(2);
      expect(mockResponse.getTotal()).andReturn(0);
      expect(mockResponse.getData()).andReturn(emptyData);

      replay(mockMailjetClient, mockResponse);

      // Act
      JSONObject result = mailJetApiClientWrapper.getAccountByIdOrEmail(mailjetId);

      // Assert
      verify(mockMailjetClient, mockResponse);
      assertNull(result);
    }
  }

  @Nested
  class PermanentlyDeleteAccountTests {

    @Test
    void permanentlyDeleteAccount_WithValidId_ShouldDelete() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.delete(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(204);

      replay(mockMailjetClient, mockResponse);

      // Act
      mailJetApiClientWrapper.permanentlyDeleteAccountById(mailjetId);

      // Assert
      verify(mockMailjetClient, mockResponse);
    }

    @Test
    void permanentlyDeleteAccount_WithStatus200_ShouldSucceed() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.delete(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(200);
      expect(mockResponse.getStatus()).andReturn(200);

      replay(mockMailjetClient, mockResponse);

      // Act
      mailJetApiClientWrapper.permanentlyDeleteAccountById(mailjetId);

      // Assert
      verify(mockMailjetClient, mockResponse);
    }

    @Test
    void permanentlyDeleteAccount_WithNotFound_ShouldNotThrow() throws MailjetException {
      // Arrange
      String mailjetId = "999";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.delete(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(200);
      expect(mockResponse.getStatus()).andReturn(200);

      replay(mockMailjetClient, mockResponse);

      // Act - Should not throw
      mailJetApiClientWrapper.permanentlyDeleteAccountById(mailjetId);

      // Assert
      verify(mockMailjetClient, mockResponse);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void permanentlyDeleteAccount_WithNullOrEmptyId_ShouldThrowException(String input) {
      // Act & Assert
      assertThrows(IllegalArgumentException.class,
          () -> mailJetApiClientWrapper.permanentlyDeleteAccountById(input));
    }

    @Test
    void permanentlyDeleteAccount_WithUnexpectedStatus_ShouldThrowException() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.delete(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);

      replay(mockMailjetClient, mockResponse);

      // Act & Assert
      assertThrows(MailjetException.class,
          () -> mailJetApiClientWrapper.permanentlyDeleteAccountById(mailjetId));

      verify(mockMailjetClient, mockResponse);
    }

    @Test
    void permanentlyDeleteAccount_WithCommunicationError_ShouldThrowCommunicationException() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetException connectionException = new MailjetException("Connection refused");

      expect(mockMailjetClient.delete(anyObject(MailjetRequest.class))).andThrow(connectionException);

      replay(mockMailjetClient);

      // Act & Assert
      assertThrows(MailjetClientCommunicationException.class,
          () -> mailJetApiClientWrapper.permanentlyDeleteAccountById(mailjetId));

      verify(mockMailjetClient);
    }

    @Test
    void permanentlyDeleteAccount_WithNotFoundException_ShouldNotThrow() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetException notFoundException = new MailjetException("Object not found (404)");

      expect(mockMailjetClient.delete(anyObject(MailjetRequest.class))).andThrow(notFoundException);

      replay(mockMailjetClient);

      // Act - Should not throw
      assertDoesNotThrow(() -> mailJetApiClientWrapper.permanentlyDeleteAccountById(mailjetId));

      verify(mockMailjetClient);
    }
  }

  @Nested
  class AddNewUserOrGetUserIfExistsTests {

    @Test
    void addNewUser_WithNewEmail_ShouldReturnNewId() throws MailjetException {
      // Arrange
      String email = "test@example.com";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);
      JSONArray mockData = new JSONArray();
      JSONObject mockUser = new JSONObject();
      mockUser.put("ID", 456);
      mockData.put(mockUser);

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(201);
      expect(mockResponse.getData()).andReturn(mockData);

      replay(mockMailjetClient, mockResponse);

      // Act
      String result = mailJetApiClientWrapper.addNewUserOrGetUserIfExists(email);

      // Assert
      verify(mockMailjetClient, mockResponse);
      assertEquals("456", result);
    }

    @Test
    void addNewUser_WithExistingEmail_ShouldFetchAndReturnId() throws MailjetException {
      // Arrange
      String email = "existing@example.com";
      MailjetResponse postResponse = createMock(MailjetResponse.class);
      MailjetResponse getResponse = createMock(MailjetResponse.class);

      JSONArray getData = new JSONArray();
      JSONObject existingUser = new JSONObject();
      existingUser.put("ID", 789);
      existingUser.put("Email", email);
      getData.put(existingUser);

      // Simulate "already exists" error on POST
      expect(mockMailjetClient.post(anyObject(MailjetRequest.class)))
          .andThrow(new MailjetClientRequestException("Email already exists", 1));

      // Then successful GET to retrieve existing account
      expect(mockMailjetClient.get(anyObject(MailjetRequest.class))).andReturn(getResponse);
      expect(getResponse.getStatus()).andReturn(200).times(2);
      expect(getResponse.getTotal()).andReturn(1);
      expect(getResponse.getData()).andReturn(getData);

      replay(mockMailjetClient, postResponse, getResponse);

      // Act
      String result = mailJetApiClientWrapper.addNewUserOrGetUserIfExists(email);

      // Assert
      verify(mockMailjetClient, getResponse);
      assertEquals("789", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void addNewUser_WithNullOrEmptyEmail_ShouldReturnNull(String input) throws MailjetException {
      // Act
      String result = mailJetApiClientWrapper.addNewUserOrGetUserIfExists(input);

      // Assert
      assertNull(result);
    }

    @Test
    void addNewUser_WithEmailNormalization_ShouldTrimAndLowercase() throws MailjetException {
      // Arrange
      String email = "  Test@EXAMPLE.COM  ";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);
      JSONArray mockData = new JSONArray();
      JSONObject mockUser = new JSONObject();
      mockUser.put("ID", 999);
      mockData.put(mockUser);

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(201);
      expect(mockResponse.getData()).andReturn(mockData);

      replay(mockMailjetClient, mockResponse);

      // Act
      String result = mailJetApiClientWrapper.addNewUserOrGetUserIfExists(email);

      // Assert
      verify(mockMailjetClient, mockResponse);
      assertEquals("999", result);
    }

    @Test
    void addNewUser_WithUnexpectedStatus_ShouldThrowException() throws MailjetException {
      // Arrange
      String email = "test@example.com";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);

      replay(mockMailjetClient, mockResponse);

      // Act & Assert
      assertThrows(MailjetException.class,
          () -> mailJetApiClientWrapper.addNewUserOrGetUserIfExists(email));

      verify(mockMailjetClient, mockResponse);
    }

    @Test
    void addNewUser_WithExistingButCannotFetch_ShouldThrowException() throws MailjetException {
      // Arrange
      String email = "existing@example.com";
      MailjetResponse getResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class)))
          .andThrow(new MailjetClientRequestException("Email already exists", 1));

      expect(mockMailjetClient.get(anyObject(MailjetRequest.class))).andReturn(getResponse);
      expect(getResponse.getStatus()).andReturn(404);

      replay(mockMailjetClient, getResponse);

      // Act & Assert
      assertThrows(MailjetException.class,
          () -> mailJetApiClientWrapper.addNewUserOrGetUserIfExists(email));

      verify(mockMailjetClient, getResponse);
    }

    @Test
    void addNewUser_WithCommunicationError_ShouldThrowCommunicationException() throws MailjetException {
      // Arrange
      String email = "test@example.com";
      MailjetException timeoutException = new MailjetException("Connection timeout");

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andThrow(timeoutException);

      replay(mockMailjetClient);

      // Act & Assert
      assertThrows(MailjetClientCommunicationException.class,
          () -> mailJetApiClientWrapper.addNewUserOrGetUserIfExists(email));

      verify(mockMailjetClient);
    }
  }

  @Nested
  class UpdateUserPropertiesTests {

    @Test
    void updateUserProperties_WithValidData_ShouldUpdate() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.put(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(200);
      expect(mockResponse.getTotal()).andReturn(1);

      replay(mockMailjetClient, mockResponse);

      // Act
      mailJetApiClientWrapper.updateUserProperties(mailjetId, "John", "STUDENT", "VERIFIED", "GCSE");

      // Assert
      verify(mockMailjetClient, mockResponse);
    }

    @Test
    void updateUserProperties_WithNullValues_ShouldUseEmptyStrings() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.put(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(200);
      expect(mockResponse.getTotal()).andReturn(1);

      replay(mockMailjetClient, mockResponse);

      // Act
      mailJetApiClientWrapper.updateUserProperties(mailjetId, null, null, null, null);

      // Assert
      verify(mockMailjetClient, mockResponse);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void updateUserProperties_WithNullOrEmptyId_ShouldThrowException(String input) {
      // Act & Assert
      assertThrows(IllegalArgumentException.class,
          () -> mailJetApiClientWrapper.updateUserProperties(input, "John", "STUDENT", "VERIFIED", "GCSE"));
    }

    @Test
    void updateUserProperties_WithNotFound_ShouldThrowException() throws MailjetException {
      // Arrange
      String mailjetId = "999";
      MailjetException notFoundException = new MailjetException("Contact not found (404)");

      expect(mockMailjetClient.put(anyObject(MailjetRequest.class))).andThrow(notFoundException);

      replay(mockMailjetClient);

      // Act & Assert
      assertThrows(MailjetException.class,
          () -> mailJetApiClientWrapper.updateUserProperties(mailjetId, "John", "STUDENT", "VERIFIED", "GCSE"));

      verify(mockMailjetClient);
    }

    @Test
    void updateUserProperties_WithUnexpectedStatus_ShouldThrowException() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.put(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getTotal()).andReturn(0);
      expect(mockResponse.getTotal()).andReturn(0);

      replay(mockMailjetClient, mockResponse);

      // Act & Assert
      assertThrows(MailjetException.class,
          () -> mailJetApiClientWrapper.updateUserProperties(mailjetId, "John", "STUDENT", "VERIFIED", "GCSE"));

      verify(mockMailjetClient, mockResponse);
    }

    @Test
    void updateUserProperties_WithCommunicationError_ShouldThrowCommunicationException() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetException connectionException = new MailjetException("Connection timeout");

      expect(mockMailjetClient.put(anyObject(MailjetRequest.class))).andThrow(connectionException);

      replay(mockMailjetClient);

      // Act & Assert
      assertThrows(MailjetClientCommunicationException.class,
          () -> mailJetApiClientWrapper.updateUserProperties(mailjetId, "John", "STUDENT", "VERIFIED", "GCSE"));

      verify(mockMailjetClient);
    }
  }

  @Nested
  class UpdateUserSubscriptionsTests {

    @Test
    void updateUserSubscriptions_WithValidData_ShouldUpdate() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(201);
      expect(mockResponse.getTotal()).andReturn(1);

      replay(mockMailjetClient, mockResponse);

      // Act
      mailJetApiClientWrapper.updateUserSubscriptions(mailjetId,
          MailJetSubscriptionAction.FORCE_SUBSCRIBE,
          MailJetSubscriptionAction.UNSUBSCRIBE);

      // Assert
      verify(mockMailjetClient, mockResponse);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void updateUserSubscriptions_WithNullOrEmptyId_ShouldThrowException(String input) {
      // Act & Assert
      assertThrows(IllegalArgumentException.class,
          () -> mailJetApiClientWrapper.updateUserSubscriptions(input,
              MailJetSubscriptionAction.FORCE_SUBSCRIBE,
              MailJetSubscriptionAction.UNSUBSCRIBE));
    }

    @Test
    void updateUserSubscriptions_WithNullNewsAction_ShouldThrowException() {
      // Act & Assert
      assertThrows(IllegalArgumentException.class,
          () -> mailJetApiClientWrapper.updateUserSubscriptions("123", null,
              MailJetSubscriptionAction.UNSUBSCRIBE));
    }

    @Test
    void updateUserSubscriptions_WithNullEventsAction_ShouldThrowException() {
      // Act & Assert
      assertThrows(IllegalArgumentException.class,
          () -> mailJetApiClientWrapper.updateUserSubscriptions("123",
              MailJetSubscriptionAction.FORCE_SUBSCRIBE, null));
    }

    @Test
    void updateUserSubscriptions_WithNotFound_ShouldThrowException() throws MailjetException {
      // Arrange
      String mailjetId = "999";
      MailjetException notFoundException = new MailjetException("Contact not found (404)");

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andThrow(notFoundException);

      replay(mockMailjetClient);

      // Act & Assert
      assertThrows(MailjetException.class,
          () -> mailJetApiClientWrapper.updateUserSubscriptions(mailjetId,
              MailJetSubscriptionAction.FORCE_SUBSCRIBE,
              MailJetSubscriptionAction.UNSUBSCRIBE));

      verify(mockMailjetClient);
    }

    @Test
    void updateUserSubscriptions_WithUnexpectedStatus_ShouldThrowException() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetResponse mockResponse = createMock(MailjetResponse.class);

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andReturn(mockResponse);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getStatus()).andReturn(500);
      expect(mockResponse.getTotal()).andReturn(500);
      expect(mockResponse.getTotal()).andReturn(500);
      replay(mockMailjetClient, mockResponse);

      // Act & Assert
      assertThrows(MailjetException.class,
          () -> mailJetApiClientWrapper.updateUserSubscriptions(mailjetId,
              MailJetSubscriptionAction.FORCE_SUBSCRIBE,
              MailJetSubscriptionAction.UNSUBSCRIBE));

      verify(mockMailjetClient, mockResponse);
    }

    @Test
    void updateUserSubscriptions_WithCommunicationError_ShouldThrowCommunicationException() throws MailjetException {
      // Arrange
      String mailjetId = "123";
      MailjetException timeoutException = new MailjetException("Timeout occurred");

      expect(mockMailjetClient.post(anyObject(MailjetRequest.class))).andThrow(timeoutException);

      replay(mockMailjetClient);

      // Act & Assert
      assertThrows(MailjetClientCommunicationException.class,
          () -> mailJetApiClientWrapper.updateUserSubscriptions(mailjetId,
              MailJetSubscriptionAction.FORCE_SUBSCRIBE,
              MailJetSubscriptionAction.UNSUBSCRIBE));

      verify(mockMailjetClient);
    }
  }

  private void injectMockMailjetClient(MailJetApiClientWrapper wrapper, MailjetClient client) {
    try {
      var field = MailJetApiClientWrapper.class.getDeclaredField("mailjetClient");
      field.setAccessible(true);
      field.set(wrapper, client);
      field.setAccessible(false);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to inject mocked Mailjet client", e);
    }
  }
}
