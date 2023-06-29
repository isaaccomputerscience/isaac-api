package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dto.LocalAuthDTO;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginByIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.eclipse.jetty.http.HttpCookie.SAME_SITE_LAX_COMMENT;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class AuthenticationFacadeIT extends IsaacIntegrationTest {
    private AuthenticationFacade authenticationFacade;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    @BeforeAll
    public static void beforeAll() {
        misuseMonitor.registerHandler(SegueLoginByEmailMisuseHandler.class.getSimpleName(),
                new SegueLoginByEmailMisuseHandler(1, 2, NUMBER_SECONDS_IN_MINUTE));
        misuseMonitor.registerHandler(SegueLoginByIPMisuseHandler.class.getSimpleName(),
                new SegueLoginByIPMisuseHandler(1, 2, NUMBER_SECONDS_IN_MINUTE));
    }

    @BeforeEach
    public void beforeEach() {
        misuseMonitor.resetMisuseCount("test-student@test.com", SegueLoginByEmailMisuseHandler.class.getSimpleName());
        misuseMonitor.resetMisuseCount("0.0.0.0", SegueLoginByIPMisuseHandler.class.getSimpleName());
        this.authenticationFacade = new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor);
        mockRequest = createMockRequestObject();
        mockResponse = niceMock(HttpServletResponse.class);
    }

    private static HttpServletRequest createMockRequestObject() {
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(mockSession.getId()).andReturn("sessionId").anyTimes();
        replay(mockSession);
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
        replay(mockRequest);
        return mockRequest;
    }

    @Test
    public void login_cookie_samesite() throws Exception {
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        assertEquals(SAME_SITE_LAX_COMMENT, teacherLogin.cookie.getComment());
    }

    // See E2E for logout test - response object is mocked in IT tests, preventing retrieval of transformed logout cookie
    // Usage of actual object may be possible in future but the complexity is currently prohibitive

    @Test
    public void resetPassword_emailRateLimits() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO targetUser = new LocalAuthDTO();
        targetUser.setEmail("test-student@test.com");
        targetUser.setPassword("123");

        Response firstResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), firstResetResponse.getStatus());

        Response secondResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), secondResetResponse.getStatus());

        Response thirdResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
    }

    @Test
    public void resetPassword_ipRateLimits() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO targetUser = new LocalAuthDTO();
        targetUser.setPassword("123");

        targetUser.setEmail("test-student@test.com");
        Response firstResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), firstResetResponse.getStatus());

        targetUser.setEmail("test-student2@test.com");
        Response secondResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), secondResetResponse.getStatus());

        targetUser.setEmail("test-student3@test.com");
        Response thirdResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
    }

    @Test
    public void userLogout_sessionDeauthentication() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        LocalAuthDTO targetUser = new LocalAuthDTO();
        targetUser.setEmail("test-student@test.com");
        targetUser.setPassword("test1234");

//        Map<String, NewCookie> firstResponseCookies = new HashMap();
        Capture<Cookie> firstResponseCookies = newCapture();
        HttpServletResponse firstLoginResponse = createMock(HttpServletResponse.class);
        firstLoginResponse.addCookie(capture(firstResponseCookies));
        replay(firstLoginResponse);

        authenticationFacade.authenticateWithCredentials(mockRequest, firstLoginResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
//        NewCookie firstLoginAuthCookie = firstLoginResponse.getCookies().get(SEGUE_AUTH_COOKIE);
        Cookie firstLoginAuthCookie = firstResponseCookies.getValues().get(0);
        Map<String, String> firstLoginSessionInformation = userAuthenticationManager.decodeCookie(firstLoginAuthCookie);
        // Session should be valid
        assertTrue(userAuthenticationManager.isSessionValid(firstLoginSessionInformation));

//        Response logoutResponse = authenticationFacade.userLogout(mockRequest, mockResponse);
//        NewCookie logoutCookie = firstLoginResponse.getCookies().get(SEGUE_AUTH_COOKIE);
//        // Should be no session associated with logout
//        assertEquals("", logoutCookie.getValue());
//        // Session should have been invalidated
//        assertFalse(userAuthenticationManager.isSessionValid(firstLoginSessionInformation));
//
//        Response secondLoginResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
//        NewCookie secondLoginAuthCookie = secondLoginResponse.getCookies().get(SEGUE_AUTH_COOKIE);
//        Map<String, String> secondLoginSessionInformation = userAuthenticationManager.decodeCookie(secondLoginAuthCookie);
//        // New session should be valid
//        assertTrue(userAuthenticationManager.isSessionValid(secondLoginSessionInformation));
//        // Previous session should still be invalid
//        assertFalse(userAuthenticationManager.isSessionValid(firstLoginSessionInformation));
//        // Sessions should have different tokens
//        assertNotEquals(firstLoginSessionInformation.get(SESSION_TOKEN), secondLoginSessionInformation.get(SESSION_TOKEN));
    }
}
