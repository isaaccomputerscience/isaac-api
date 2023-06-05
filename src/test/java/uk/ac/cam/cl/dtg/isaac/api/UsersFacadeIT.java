package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;
import uk.ac.cam.cl.dtg.segue.api.monitors.RegistrationMisuseHandler;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.stream.Stream;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_USER;

public class UsersFacadeIT extends IsaacIntegrationJupiterTest {
    private UsersFacade usersFacade;

    @BeforeAll
    public static void beforeAll() {
        misuseMonitor.registerHandler(RegistrationMisuseHandler.class.getSimpleName(),
                new RegistrationMisuseHandler(emailManager, properties));
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        misuseMonitor.resetMisuseCount("0.0.0.0", RegistrationMisuseHandler.class.getSimpleName());
        this.usersFacade = new UsersFacade(properties, userAccountManager, logManager, userAssociationManager, misuseMonitor, userPreferenceManager, schoolListReader);
    }

    @ParameterizedTest
    @MethodSource("validRegistrationParameters")
    public void createUser_validRegistrationParameters(String email, String password, String dob, String familyName, String givenName) {
        System.out.println("ParameterizedTest");
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(mockSession.getId()).andReturn("sessionIdFailure");
        replay(mockSession);
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
        replay(mockRequest);
        HttpServletResponse mockResponse = niceMock(HttpServletResponse.class);

        String userObjectString = String.format("{\"registeredUser\":{\"loggedIn\":true,\"email\":\"%1$s\",\"dateOfBirth\":\"%3$s\",\"password\":\"%2$s\",\"familyName\":\"%4$s\",\"givenName\":\"%5$s\"},\"userPreferences\":{},\"passwordCurrent\":null}",
                email, password, dob, familyName, givenName);

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

    private static Stream<Arguments> validRegistrationParameters() {
        // Email, Password, DOB, familyName, givenName
        return Stream.of(
                Arguments.of("new-student@test.com", "password", "2000-01-01T00:00:00.000Z", "Test", "Test"),
                Arguments.of("new-student-google", "password", "2000-01-01T00:00:00.000Z", "Test", "Test")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrationParameters")
    public void createUser_invalidRegistrationParameters(String email, String password, String dob, String familyName, String givenName) {
        System.out.println("ParameterizedTest");
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(mockSession.getId()).andReturn("sessionIdFailure");
        replay(mockSession);
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
        replay(mockRequest);
        HttpServletResponse mockResponse = niceMock(HttpServletResponse.class);

        String userObjectString = String.format("{\"registeredUser\":{\"loggedIn\":true,\"email\":\"%1$s\",\"dateOfBirth\":\"%3$s\",\"password\":\"%2$s\",\"familyName\":\"%4$s\",\"givenName\":\"%5$s\"},\"userPreferences\":{},\"passwordCurrent\":null}",
                email, password, dob, familyName, givenName);

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

    private static Stream<Arguments> invalidRegistrationParameters() {
        // Email, Password, DOB, familyName, givenName
        return Stream.of(
//                Arguments.of("new-student@test.com", "password", "2000-01-01T00:00:00.000Z", "Test", "Test"),
                // Isaac addresses are forbidden
                Arguments.of("new-student@isaaccomputerscience.org", "password", "2000-01-01T00:00:00.000Z", "Test", "Test"),
                // Email field cannot be empty
                Arguments.of("", "password", "2000-01-01T00:00:00.000Z", "Test", "Test"),
                // Email field cannot have conecutive .s
                Arguments.of("new-student@test..com", "password", "2000-01-01T00:00:00.000Z", "Test", "Test"),
                // Email field must contain an @
                Arguments.of("new-student.test.com", "password", "2000-01-01T00:00:00.000Z", "Test", "Test"),
                // Email field must match an @x.y pattern
                Arguments.of("new-student@testcom", "password", "2000-01-01T00:00:00.000Z", "Test", "Test"),
                // Family name cannot be empty
                Arguments.of("new-student@test.com", "password", "2000-01-01T00:00:00.000Z", "", "Test"),
                // Family name cannot exceed maximum length
                Arguments.of("new-student@test.com", "password", "2000-01-01T00:00:00.000Z", "01abcdefghijklmn02abcdefghijklmn03abcdefghijklmn04abcdefghijklmn05abcdefghijklmn06abcdefghijklmn07abcdefghijklmn08abcdefghijklmn09abcdefghijklmn10abcdefghijklmn11abcdefghijklmn12abcdefghijklmn13abcdefghijklmn14abcdefghijklmn15abcdefghijklmn16abcdefghijklmn", "Test"),
                // Family name cannot contain forbidden characters
                Arguments.of("new-student@test.com", "password", "2000-01-01T00:00:00.000Z", "Te*st", "Test"),
                // Given name cannot be empty
                Arguments.of("new-student@test.com", "password", "2000-01-01T00:00:00.000Z", "Test", ""),
                // Given name cannot exceed maximum length
                Arguments.of("new-student@test.com", "password", "2000-01-01T00:00:00.000Z", "Test", "01abcdefghijklmn02abcdefghijklmn03abcdefghijklmn04abcdefghijklmn05abcdefghijklmn06abcdefghijklmn07abcdefghijklmn08abcdefghijklmn09abcdefghijklmn10abcdefghijklmn11abcdefghijklmn12abcdefghijklmn13abcdefghijklmn14abcdefghijklmn15abcdefghijklmn16abcdefghijklmn"),
                // Given name cannot contain forbidden characters
                Arguments.of("new-student@test.com", "password", "2000-01-01T00:00:00.000Z", "Test", "Te*st"),
                // Password cannot be empty
                Arguments.of("new-student@test.com", "", "2000-01-01T00:00:00.000Z", "Test", "Test"),
                // Password must be at least minimum length
                Arguments.of("new-student@test.com", "pass", "2000-01-01T00:00:00.000Z", "Test", "Test")
        );
    }
}
