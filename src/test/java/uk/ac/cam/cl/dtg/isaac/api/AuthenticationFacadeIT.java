package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dto.LocalAuthDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginbyEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginbyIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.easymock.EasyMock.*;
import static org.eclipse.jetty.http.HttpCookie.SAME_SITE_LAX_COMMENT;
import static org.junit.Assert.assertEquals;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_USER;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_MINUTE;

public class AuthenticationFacadeIT extends IsaacIntegrationTest {
    private AuthenticationFacade authenticationFacade;

    @BeforeClass
    public static void beforeAll() {
        misuseMonitor.registerHandler(SegueLoginbyEmailMisuseHandler.class.getSimpleName(),
                new SegueLoginbyEmailMisuseHandler(emailManager, properties, 1, 2, NUMBER_SECONDS_IN_MINUTE));
        misuseMonitor.registerHandler(SegueLoginbyIPMisuseHandler.class.getSimpleName(),
                new SegueLoginbyIPMisuseHandler(emailManager, properties, 1, 2, NUMBER_SECONDS_IN_MINUTE));
    }

    @Before
    public void beforeEach() throws Exception {
        misuseMonitor.resetMisuseCount("test-student@test.com", SegueLoginbyEmailMisuseHandler.class.getSimpleName());
        misuseMonitor.resetMisuseCount("0.0.0.0", SegueLoginbyIPMisuseHandler.class.getSimpleName());
        this.authenticationFacade = new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor);
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
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(mockSession.getId()).andReturn("sessionIdEmail1").andReturn("sessionIdEmail2").andReturn("sessionIdEmail3");
        replay(mockSession);
        HttpServletRequest mockAuthRequest = createNiceMock(HttpServletRequest.class);
        expect(mockAuthRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockAuthRequest.getSession()).andReturn(mockSession).anyTimes();
        replay(mockAuthRequest);
        HttpServletResponse  mockAuthResponse = createNiceMock(HttpServletResponse.class);

        LocalAuthDTO targetUser = new LocalAuthDTO();
        targetUser.setEmail("test-student@test.com");
        targetUser.setPassword("123");

        Response firstResetResponse = authenticationFacade.authenticateWithCredentials(mockAuthRequest, mockAuthResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), firstResetResponse.getStatus());

        Response secondResetResponse = authenticationFacade.authenticateWithCredentials(mockAuthRequest, mockAuthResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), secondResetResponse.getStatus());

        Response thirdResetResponse = authenticationFacade.authenticateWithCredentials(mockAuthRequest, mockAuthResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
    }

    @Test
    public void resetPassword_ipRateLimits() throws InvalidKeySpecException, NoSuchAlgorithmException {
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(mockSession.getId()).andReturn("sessionIdIp1").andReturn("sessionIdIp2").andReturn("sessionIdIp3");
        replay(mockSession);
        HttpServletRequest mockAuthRequest = createNiceMock(HttpServletRequest.class);
        expect(mockAuthRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockAuthRequest.getSession()).andReturn(mockSession).anyTimes();
        replay(mockAuthRequest);
        HttpServletResponse  mockAuthResponse = createNiceMock(HttpServletResponse.class);

        LocalAuthDTO targetUser = new LocalAuthDTO();
        targetUser.setPassword("123");

        targetUser.setEmail("test-student@test.com");
        Response firstResetResponse = authenticationFacade.authenticateWithCredentials(mockAuthRequest, mockAuthResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), firstResetResponse.getStatus());

        targetUser.setEmail("test-student2@test.com");
        Response secondResetResponse = authenticationFacade.authenticateWithCredentials(mockAuthRequest, mockAuthResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), secondResetResponse.getStatus());

        targetUser.setEmail("test-student3@test.com");
        Response thirdResetResponse = authenticationFacade.authenticateWithCredentials(mockAuthRequest, mockAuthResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
    }
}
