package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByIPMisuseHandler;

import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_USER;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_MINUTE;

public class UsersFacadeIT extends IsaacIntegrationTest {
    private UsersFacade usersFacade;

    @BeforeClass
    public static void beforeAll() {
        misuseMonitor.registerHandler(PasswordResetByEmailMisuseHandler.class.getSimpleName(),
                new PasswordResetByEmailMisuseHandler(1, 2, NUMBER_SECONDS_IN_MINUTE));
        misuseMonitor.registerHandler(PasswordResetByIPMisuseHandler.class.getSimpleName(),
                new PasswordResetByIPMisuseHandler(emailManager, properties, 1, 2, NUMBER_SECONDS_IN_MINUTE));
    }

    @Before
    public void beforeEach() throws Exception {
        misuseMonitor.resetMisuseCount("test-student@test.com", PasswordResetByEmailMisuseHandler.class.getSimpleName());
        misuseMonitor.resetMisuseCount("0.0.0.0", PasswordResetByIPMisuseHandler.class.getSimpleName());
        this.usersFacade = new UsersFacade(properties, userAccountManager, logManager, userAssociationManager, misuseMonitor, userPreferenceManager, schoolListReader);
    }

    private static HttpServletRequest createMockRequestObject(List<String> sessionIds) {
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        sessionIds.forEach(id -> expect(mockSession.getId()).andReturn(id));
        replay(mockSession);
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
        replay(mockRequest);
        return mockRequest;
    }

    @Test
    public void resetPassword_emailRateLimits() {
        HttpServletRequest mockRequest = createMockRequestObject(List.of("sessionIdEmail1", "sessionIdEmail2", "sessionIdEmail3"));

        RegisteredUserDTO targetUser = new RegisteredUserDTO();
        targetUser.setEmail("test-student@test.com");

        Response firstResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockRequest);
        assertEquals(Response.Status.OK.getStatusCode(), firstResetResponse.getStatus());

        Response secondResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockRequest);
        assertEquals(Response.Status.OK.getStatusCode(), secondResetResponse.getStatus());

        Response thirdResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockRequest);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
    }

    @Test
    public void resetPassword_ipRateLimits() {
        HttpServletRequest mockRequest = createMockRequestObject(List.of("sessionIdIp1", "sessionIdIp2", "sessionIdIp3"));

        RegisteredUserDTO targetUser = new RegisteredUserDTO();

        targetUser.setEmail("test-student@test.com");
        Response firstResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockRequest);
        assertEquals(Response.Status.OK.getStatusCode(), firstResetResponse.getStatus());

        targetUser.setEmail("test-student2@test.com");
        Response secondResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockRequest);
        assertEquals(Response.Status.OK.getStatusCode(), secondResetResponse.getStatus());

        targetUser.setEmail("test-student3@test.com");
        Response thirdResetResponse = usersFacade.generatePasswordResetToken(targetUser, mockRequest);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
    }
}
