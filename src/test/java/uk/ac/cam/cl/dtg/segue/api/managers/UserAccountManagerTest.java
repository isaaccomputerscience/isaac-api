package uk.ac.cam.cl.dtg.segue.api.managers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.cam.cl.dtg.isaac.dos.users.Role.STUDENT;
import static uk.ac.cam.cl.dtg.isaac.dos.users.Role.TEACHER;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager.isEmailValid;
import static uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager.isUserNameValid;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

public class UserAccountManagerTest {

  PgUsers database;
  QuestionManager questionmanager;
  PropertiesLoader propertiesLoader;
  Map<AuthenticationProvider, IAuthenticator> providersToRegister;
  MapperFacade dtoMapper;
  EmailManager emailManager;
  PgAnonymousUsers pgAnonymousUsers;
  ILogManager logManager;
  UserAuthenticationManager userAuthenticationManager;
  ISecondFactorAuthenticator secondFactorAuthenticator;
  PgUserPreferenceManager userPreferenceManager;
  SchoolListReader schoolListReader;
  UserAccountManager userAccountManager;

  @BeforeEach
  public void beforeEach() {
    database = createMock(PgUsers.class);
    questionmanager = createNiceMock(QuestionManager.class);
    propertiesLoader = createNiceMock(PropertiesLoader.class);
    providersToRegister = Map.of();
    dtoMapper = createNiceMock(MapperFacade.class);
    emailManager = createMock(EmailManager.class);
    pgAnonymousUsers = createNiceMock(PgAnonymousUsers.class);
    logManager = createNiceMock(ILogManager.class);
    userAuthenticationManager = createNiceMock(UserAuthenticationManager.class);
    secondFactorAuthenticator = createNiceMock(ISecondFactorAuthenticator.class);
    userPreferenceManager = createNiceMock(PgUserPreferenceManager.class);
    schoolListReader = createMock(SchoolListReader.class);

    expect(propertiesLoader.getProperty(HMAC_SALT)).andStubReturn("SALTY");
    expect(propertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS_DEFAULT)).andStubReturn("60");
    expect(propertiesLoader.getProperty(Constants.HOST_NAME)).andStubReturn("HOST");
    expect(propertiesLoader.getProperty(Constants.MAIL_RECEIVERS)).andReturn("admin@localhost");
    replay(propertiesLoader);

    userAccountManager =
        new UserAccountManager(database, questionmanager, propertiesLoader, providersToRegister, dtoMapper,
            emailManager, pgAnonymousUsers, logManager, userAuthenticationManager, secondFactorAuthenticator,
            userPreferenceManager, schoolListReader);
  }

  @AfterEach
  public void afterEach() {
    reset(database);
    reset(schoolListReader);
    reset(emailManager);
  }

  @ParameterizedTest
  @MethodSource("namesToTest")
  public void nameValidation(boolean expectedIsValid, String name) {
    boolean actualIsValid = isUserNameValid(name);
    assertEquals(expectedIsValid, actualIsValid);
  }

  private static Stream<Arguments> namesToTest() {
    return Stream.of(
        Arguments.of(true, "testname"), // Lowercase is valid
        Arguments.of(true, "TESTNAME"), // Uppercase is valid
        Arguments.of(true, "TestName"), // Mixture of cases is valid
        Arguments.of(true, "TestName123"), // Numbers are valid
        Arguments.of(true, "Test_Name"), // Underscores are valid
        Arguments.of(true, "Test Name"), // Double-barrelled names with spaces are valid
        Arguments.of(true, "Test-Name"), // Double-barrelled names with hyphens are valid
        Arguments.of(true, "O'Name"), // Apostrophes are valid
        // Accented and non-English characters are valid.
        // Please note, this is not an exhustive test - unicode supports thousands of such characters
        Arguments.of(true, "ÃëóûÿĉĐŗǥȕḍṦμϱнӯ"),
        Arguments.of(false, null), // Null is not valid
        Arguments.of(false, ""), // Empty string is not valid
        Arguments.of(false, " "), // Only whitespace is not valid
        Arguments.of(false, "a".repeat(256)), // Names exceeding the maximum length of 255 characters are not valid
        // Other special characters are not permitted
        Arguments.of(false, "Test!Name"),
        Arguments.of(false, "Test?Name"),
        Arguments.of(false, "Test#Name"),
        Arguments.of(false, "Test.Name"),
        Arguments.of(false, "Test,Name"),
        Arguments.of(false, "Test*Name"),
        Arguments.of(false, "Test<Name"),
        Arguments.of(false, "Test>Name"),
        Arguments.of(false, "Test:Name"),
        Arguments.of(false, "Test;Name"),
        Arguments.of(false, "Test/Name")
    );
  }

  @ParameterizedTest
  @MethodSource("emailsToTest")
  public void emailValidation(boolean expectedIsValid, String email) {
    boolean actualIsValid = isEmailValid(email);
    assertEquals(expectedIsValid, actualIsValid);
  }

  private static Stream<Arguments> emailsToTest() {
    return Stream.of(
        Arguments.of(true, "testemail@test.com"), // Standard email format is valid
        // Alternate pattern for twitter|google|facebook is valid
        Arguments.of(true, "testemail-twitter"),
        Arguments.of(true, "testemail-google"),
        Arguments.of(true, "testemail-facebook"),
        Arguments.of(true, "test!#$%&'+-=?^_`.{|}~email@test.com"), // Some special characters are permitted
        Arguments.of(true, "testemail@test.co.uk"), // Multiple seperated .s are valid
        Arguments.of(false, null), // Null is not valid
        Arguments.of(false, ""), // Empty string is not valid
        Arguments.of(false, " "), // Only whitespace is not valid
        Arguments.of(false, "test.email@testcom"), // Email must have at least one . after the @
        Arguments.of(false, "testemailtest.com"), // Standard email must include an @
        Arguments.of(false, "testemail@test."), // Email must have be at least character after the last .
        Arguments.of(false, "testemail@.com"), // Email must have at least one character between the @ and the last .
        Arguments.of(false, "testemail@test..com"), // Email cannot contain consecutive full stops
        // Other special characters are not permitted
        Arguments.of(false, "test\"email@test.com"),
        Arguments.of(false, "test(email@test.com"),
        Arguments.of(false, "test)email@test.com"),
        Arguments.of(false, "test*email@test.com"),
        Arguments.of(false, "test/email@test.com"),
        Arguments.of(false, "test<email@test.com"),
        Arguments.of(false, "test>email@test.com"),
        Arguments.of(false, "test:email@test.com"),
        Arguments.of(false, "test;email@test.com"),
        // Other alternate endings are not permitted
        Arguments.of(false, "testemail-example"),
        Arguments.of(false, "testemail-twittter")
    );
  }

  @Test
  public void updateTeacherPendingFlag_success() throws SegueDatabaseException, NoUserException {
    RegisteredUser initialUserState = new RegisteredUser() {
      {
        setId(1L);
        setTeacherPending(false);
      }
    };
    RegisteredUser expectedUserState = new RegisteredUser() {
      {
        setId(1L);
        setTeacherPending(true);
      }
    };
    expect(database.getById(1L)).andReturn(initialUserState);
    expect(database.createOrUpdateUser(expectedUserState)).andStubReturn(expectedUserState);
    replay(database);

    userAccountManager.updateTeacherPendingFlag(1L, true);

    verify(database);
  }

  @Test
  public void updateTeacherPendingFlag_missingUser() throws SegueDatabaseException {
    expect(database.getById(1L)).andReturn(null);
    replay(database);

    assertThrows(NoUserException.class, () -> userAccountManager.updateTeacherPendingFlag(1L, true));

    verify(database);
  }

  @Test
  public void sendRoleChangeRequestEmail_success()
      throws UnableToIndexSchoolsException, ContentManagerException, IOException, SegueDatabaseException,
      MissingRequiredFieldException {
    School school = new School() {
      {
        setUrn("1");
        setName("SchoolName");
        setPostcode("Postcode");
      }
    };
    expect(schoolListReader.findSchoolById("1")).andReturn(school);
    replay(schoolListReader);

    Map<String, Object> expectedEmailDetails = Map.of(
        "contactGivenName", "GivenName",
        "contactFamilyName", "FamilyName",
        "contactUserId", 1L,
        "contactUserRole", STUDENT,
        "contactEmail", "test@test.com",
        "contactSubject", "Teacher Account Request",
        "contactMessage", "Hello,\n<br>\n<br>"
            + "Please could you convert my Isaac account into a teacher account.\n<br>\n<br>"
            + "My school is: SchoolName, Postcode\n<br>"
            + "A link to my school website with a staff list showing my name and email"
            + " (or a phone number to contact the school) is: school staff url\n<br>\n<br>\n<br>"
            + "Any other information: more information\n<br>\n<br>"
            + "Thanks, \n<br>\n<br>GivenName FamilyName",
        "replyToName", "GivenName FamilyName"
    );
    emailManager.sendContactUsFormEmail("admin@localhost", expectedEmailDetails);
    expectLastCall();
    replay(emailManager);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    RegisteredUserDTO user = new RegisteredUserDTO() {
      {
        setId(1L);
        setSchoolId("1");
        setGivenName("GivenName");
        setFamilyName("FamilyName");
        setEmail("test@test.com");
        setRole(STUDENT);
      }
    };
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url",
        "otherDetails", "more information"
    );

    userAccountManager.sendRoleChangeRequestEmail(request, user, TEACHER, requestDetails);

    verify(schoolListReader);
    verify(emailManager);
  }

  @Test
  public void sendRoleChangeRequestEmail_successNoOtherDetails()
      throws UnableToIndexSchoolsException, ContentManagerException, IOException, SegueDatabaseException,
      MissingRequiredFieldException {
    School school = new School() {
      {
        setUrn("1");
        setName("SchoolName");
        setPostcode("Postcode");
      }
    };
    expect(schoolListReader.findSchoolById("1")).andReturn(school);
    replay(schoolListReader);

    Map<String, Object> expectedEmailDetails = Map.of(
        "contactGivenName", "GivenName",
        "contactFamilyName", "FamilyName",
        "contactUserId", 1L,
        "contactUserRole", STUDENT,
        "contactEmail", "test@test.com",
        "contactSubject", "Teacher Account Request",
        "contactMessage", "Hello,\n<br>\n<br>"
            + "Please could you convert my Isaac account into a teacher account.\n<br>\n<br>"
            + "My school is: SchoolName, Postcode\n<br>"
            + "A link to my school website with a staff list showing my name and email"
            + " (or a phone number to contact the school) is: school staff url\n<br>\n<br>\n<br>"
            + "Thanks, \n<br>\n<br>GivenName FamilyName",
        "replyToName", "GivenName FamilyName"
    );
    emailManager.sendContactUsFormEmail("admin@localhost", expectedEmailDetails);
    expectLastCall();
    replay(emailManager);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    RegisteredUserDTO user = new RegisteredUserDTO() {
      {
        setId(1L);
        setSchoolId("1");
        setGivenName("GivenName");
        setFamilyName("FamilyName");
        setEmail("test@test.com");
        setRole(STUDENT);
      }
    };
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url"
    );

    userAccountManager.sendRoleChangeRequestEmail(request, user, TEACHER, requestDetails);

    verify(schoolListReader);
    verify(emailManager);
  }

  @Test
  public void sendRoleChangeRequestEmail_missingSchool()
      throws UnableToIndexSchoolsException, SegueSearchException, IOException {
    expect(schoolListReader.findSchoolById("1")).andReturn(null);
    replay(schoolListReader);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    RegisteredUserDTO user = new RegisteredUserDTO() {
      {
        setId(1L);
        setSchoolId("1");
        setGivenName("GivenName");
        setFamilyName("FamilyName");
        setEmail("test@test.com");
        setRole(STUDENT);
      }
    };
    Map<String, String> requestDetails = Map.of(
        "verificationDetails", "school staff url",
        "otherDetails", "more information"
    );

    assertThrows(MissingRequiredFieldException.class,
        () -> userAccountManager.sendRoleChangeRequestEmail(request, user, TEACHER, requestDetails));

    verify(schoolListReader);
  }

  @ParameterizedTest(name = "{index} {0}")
  @MethodSource("sendRoleChangeRequestEmail_invalidVerificationDetails")
  public void sendRoleChangeRequestEmail_invalidVerificationDetails(String ignoredTestLabel, Map<String, String> requestDetails)
      throws UnableToIndexSchoolsException, SegueSearchException, IOException {
    School school = new School() {
      {
        setUrn("1");
        setName("SchoolName");
        setPostcode("Postcode");
      }
    };
    expect(schoolListReader.findSchoolById("1")).andReturn(school);
    replay(schoolListReader);

    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    RegisteredUserDTO user = new RegisteredUserDTO() {
      {
        setId(1L);
        setSchoolId("1");
        setGivenName("GivenName");
        setFamilyName("FamilyName");
        setEmail("test@test.com");
        setRole(STUDENT);
      }
    };

    assertThrows(MissingRequiredFieldException.class,
        () -> userAccountManager.sendRoleChangeRequestEmail(request, user, TEACHER, requestDetails));

    verify(schoolListReader);
  }

  private static Stream<Arguments> sendRoleChangeRequestEmail_invalidVerificationDetails() {
    return Stream.of(
        Arguments.of("missingVerificationDetails", Map.of(
            "otherDetails", "more information"
        )),
        Arguments.of("nullVerificationDetails", new HashMap<>() {
              {
                put("verificationDetails", null);
                put("otherDetails", "more information");
              }
            }),
        Arguments.of("emptyVerificationDetails", Map.of(
                "verificationDetails", "",
            "otherDetails", "more information"
        ))
    );
  }
}
