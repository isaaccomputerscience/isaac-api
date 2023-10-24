package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_USER;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_MINUTE;
import static uk.ac.cam.cl.dtg.util.ServletTestUtils.createMockServletRequest;
import static uk.ac.cam.cl.dtg.util.ServletTestUtils.replayMockServletRequest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.RegistrationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public class UsersFacadeIT extends IsaacIntegrationTest {
  private UsersFacade usersFacade;
  private HttpServletRequest mockRequest;
  private HttpServletResponse mockResponse;

  @BeforeAll
  public static void beforeAll() {
    misuseMonitor.registerHandler(PasswordResetByEmailMisuseHandler.class.getSimpleName(),
        new PasswordResetByEmailMisuseHandler(1, 2, NUMBER_SECONDS_IN_MINUTE));
    misuseMonitor.registerHandler(PasswordResetByIPMisuseHandler.class.getSimpleName(),
        new PasswordResetByIPMisuseHandler(emailManager, properties, 1, 2, NUMBER_SECONDS_IN_MINUTE));
    misuseMonitor.registerHandler(RegistrationMisuseHandler.class.getSimpleName(),
        new RegistrationMisuseHandler(emailManager, properties));
  }

  @BeforeEach
  public void beforeEach() {
    misuseMonitor.resetMisuseCount("test-student@test.com", PasswordResetByEmailMisuseHandler.class.getSimpleName());
    misuseMonitor.resetMisuseCount("0.0.0.0", PasswordResetByIPMisuseHandler.class.getSimpleName());
    misuseMonitor.resetMisuseCount("0.0.0.0", RegistrationMisuseHandler.class.getSimpleName());
    this.usersFacade =
        new UsersFacade(properties, userAccountManager, recaptchaManager, logManager, userAssociationManager,
            misuseMonitor, userPreferenceManager, schoolListReader);
    mockRequest = replayMockServletRequest();
    mockResponse = niceMock(HttpServletResponse.class);
  }

  private static HttpServletRequest replayMultiSessionServletRequest(List<String> sessionIds) {
    HttpSession mockSession = createNiceMock(HttpSession.class);
    expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
    sessionIds.forEach(id -> expect(mockSession.getId()).andReturn(id));
    replay(mockSession);
    HttpServletRequest mockRequest = createMockServletRequest(mockSession);
    replay(mockRequest);
    return mockRequest;
  }

  @Test
  public void resetPassword_emailRateLimits() {
    HttpServletRequest mockResetRequest =
        replayMultiSessionServletRequest(List.of("sessionIdEmail1", "sessionIdEmail2", "sessionIdEmail3"));

    RegisteredUserDTO targetUser = new RegisteredUserDTO();
    targetUser.setEmail("test-student@test.com");

    Response firstResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockResetRequest);
    assertEquals(Response.Status.OK.getStatusCode(), firstResetResponse.getStatus());

    Response secondResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockResetRequest);
    assertEquals(Response.Status.OK.getStatusCode(), secondResetResponse.getStatus());

    Response thirdResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockResetRequest);
    assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
  }

  @Test
  public void resetPassword_ipRateLimits() {
    HttpServletRequest mockResetRequest =
        replayMultiSessionServletRequest(List.of("sessionIdIp1", "sessionIdIp2", "sessionIdIp3"));

    RegisteredUserDTO targetUser = new RegisteredUserDTO();

    targetUser.setEmail("test-student1@test.com");
    Response firstResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockResetRequest);
    assertEquals(Response.Status.OK.getStatusCode(), firstResetResponse.getStatus());

    targetUser.setEmail("test-student2@test.com");
    Response secondResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockResetRequest);
    assertEquals(Response.Status.OK.getStatusCode(), secondResetResponse.getStatus());

    targetUser.setEmail("test-student3@test.com");
    Response thirdResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockResetRequest);
    assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
  }

  @ParameterizedTest
  @MethodSource("validEmailProviders")
  public void createUser_validRegistrationParameters(String email) {
    String userObjectString = String.format(
        "{\"registeredUser\":{\"loggedIn\":true,\"email\":\"%1$s\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"Password123!\",\"familyName\":\"Test\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null,\"recaptchaToken\":\"test-token\"}",
        email);

    Response response = null;
    try {
      response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
    } catch (InvalidKeySpecException e) {
      fail("Unhandled InvalidKeySpecException during test");
    } catch (NoSuchAlgorithmException e) {
      fail("Unhandled NoSuchAlgorithmException during test");
    }
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  private static Stream<Arguments> validEmailProviders() {
    return Stream.of(
        Arguments.of("new-student@test.com"),
        Arguments.of("new-student-google"),
        Arguments.of("new-student-twitter"),
        Arguments.of("new-student-facebook")
    );
  }

  @Test
  public void createUser_existingAccount() {
    String userObjectString =
        "{\"registeredUser\":{\"loggedIn\":true,\"email\":\"test-student@test.com\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"Password123!\",\"familyName\":\"Test\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null,\"recaptchaToken\":\"test-token\"}";

    Response response = null;
    try {
      response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
    } catch (InvalidKeySpecException e) {
      fail("Unhandled InvalidKeySpecException during test");
    } catch (NoSuchAlgorithmException e) {
      fail("Unhandled NoSuchAlgorithmException during test");
    }
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest
  @MethodSource("invalidEmails")
  public void createUser_invalidEmail(String email) {
    String userObjectString = String.format(
        "{\"registeredUser\":{\"loggedIn\":true,\"email\":\"%1$s\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"Password123!\",\"familyName\":\"Test\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null,\"recaptchaToken\":\"test-token\"}",
        email);

    Response response = null;
    try {
      response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
    } catch (InvalidKeySpecException e) {
      fail("Unhandled InvalidKeySpecException during test");
    } catch (NoSuchAlgorithmException e) {
      fail("Unhandled NoSuchAlgorithmException during test");
    }
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest
  @MethodSource("invalidPasswords")
  public void createUser_invalidPassword(String password) {
    String userObjectString = String.format(
        "{\"registeredUser\":{\"loggedIn\":true,\"email\":\"new-student@test.com\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"%1$s\",\"familyName\":\"Test\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null,\"recaptchaToken\":\"test-token\"}",
        password);

    Response response = null;
    try {
      response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
    } catch (InvalidKeySpecException e) {
      fail("Unhandled InvalidKeySpecException during test");
    } catch (NoSuchAlgorithmException e) {
      fail("Unhandled NoSuchAlgorithmException during test");
    }
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  public void createUser_invalidFamilyName(String familyName) {
    String userObjectString = String.format(
        "{\"registeredUser\":{\"loggedIn\":true,\"email\":\"new-student@test.com\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"Password123!\",\"familyName\":\"%1$s\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null,\"recaptchaToken\":\"test-token\"}",
        familyName);

    Response response = null;
    try {
      response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
    } catch (InvalidKeySpecException e) {
      fail("Unhandled InvalidKeySpecException during test");
    } catch (NoSuchAlgorithmException e) {
      fail("Unhandled NoSuchAlgorithmException during test");
    }
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  public void createUser_invalidGivenName(String givenName) {
    String userObjectString = String.format(
        "{\"registeredUser\":{\"loggedIn\":true,\"email\":\"new-student@test.com\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"Password123!\",\"familyName\":\"Test\",\"givenName\":\"%1$s\"},\"userPreferences\":{},\"passwordCurrent\":null,\"recaptchaToken\":\"test-token\"}",
        givenName);

    Response response = null;
    try {
      response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
    } catch (InvalidKeySpecException e) {
      fail("Unhandled InvalidKeySpecException during test");
    } catch (NoSuchAlgorithmException e) {
      fail("Unhandled NoSuchAlgorithmException during test");
    }
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  private static Stream<Arguments> invalidEmails() {
    return Stream.of(
        // Isaac addresses are forbidden
        Arguments.of("new-student@isaaccomputerscience.org"),
        // Email field cannot be empty
        Arguments.of(""),
        // Email field cannot have conecutive .s
        Arguments.of("new-student@test..com"),
        // Email field must contain an @
        Arguments.of("new-student.test.com"),
        // Email field must match an @x.y pattern
        Arguments.of("new-student@testcom")
    );
  }

  private static Stream<Arguments> invalidPasswords() {
    return Stream.of(
        // Password cannot be empty
        Arguments.of(""),
        // Password must be at least minimum length
        Arguments.of("pass")
    );
  }

  private static Stream<Arguments> invalidNames() {
    return Stream.of(
        // Names cannot be empty
        Arguments.of(""),
        // Names cannot exceed maximum length
        Arguments.of("a".repeat(256)),
        // Names cannot contain forbidden characters
        Arguments.of("Te*st")
    );
  }

  @Test
  public void requestRoleChange_valid() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    LoginResult currentlyStudentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
    HttpServletRequest upgradeRequest = createRequestWithCookies(new Cookie[] {currentlyStudentLogin.cookie});
    replay(upgradeRequest);
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url",
        "otherDetails", "more information"
    );

    Response upgradeResponse = usersFacade.requestRoleChange(upgradeRequest, requestDetails);

    assertEquals(Response.Status.OK.getStatusCode(), upgradeResponse.getStatus());
  }

  @Test
  public void requestRoleChange_missingDetails() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    LoginResult currentlyStudentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
    HttpServletRequest upgradeRequest = createRequestWithCookies(new Cookie[]{currentlyStudentLogin.cookie});
    replay(upgradeRequest);
    Map<String, String> requestDetails = Map.of(
        "otherDetails", "more information"
    );

    Response upgradeResponse = usersFacade.requestRoleChange(upgradeRequest, requestDetails);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), upgradeResponse.getStatus());
    assertEquals("Missing form details.", upgradeResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  public void requestRoleChange_notLoggedIn() {
    HttpServletRequest upgradeRequest = createRequestWithCookies(new Cookie[]{});
    replay(upgradeRequest);
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url",
        "otherDetails", "more information"
    );

    Response upgradeResponse = usersFacade.requestRoleChange(upgradeRequest, requestDetails);

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), upgradeResponse.getStatus());
    assertEquals("You must be logged in to request a role change.", upgradeResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  public void requestRoleChange_duplicateRequest()
      throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    LoginResult pendingTeacherLogin = loginAs(httpSession, ITConstants.TEST_PENDING_TEACHER_EMAIL, ITConstants.TEST_PENDING_TEACHER_PASSWORD);
    HttpServletRequest upgradeRequest = createRequestWithCookies(new Cookie[]{pendingTeacherLogin.cookie});
    replay(upgradeRequest);
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url",
        "otherDetails", "more information"
    );

    Response upgradeResponse = usersFacade.requestRoleChange(upgradeRequest, requestDetails);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), upgradeResponse.getStatus());
    assertEquals("You have already submitted a teacher upgrade request.", upgradeResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  public void requestRoleChange_alreadyTeacher()
      throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    LoginResult currentlyStudentLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL, ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest upgradeRequest = createRequestWithCookies(new Cookie[]{currentlyStudentLogin.cookie});
    replay(upgradeRequest);
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url",
        "otherDetails", "more information"
    );

    Response upgradeResponse = usersFacade.requestRoleChange(upgradeRequest, requestDetails);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), upgradeResponse.getStatus());
    assertEquals("You already have a teacher role.", upgradeResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
  }
}
