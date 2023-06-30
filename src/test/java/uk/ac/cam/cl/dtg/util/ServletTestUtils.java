package uk.ac.cam.cl.dtg.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.easymock.EasyMock.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_USER;

public class ServletTestUtils {
    public static HttpServletRequest createMockServletRequest(HttpSession mockSession) {
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
        return mockRequest;
    }

    public static HttpSession createMockSession() {
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(mockSession.getId()).andReturn("sessionId").anyTimes();
        return mockSession;
    }

    public static HttpServletRequest replayMockServletRequest() {
        HttpSession mockSession = createMockSession();
        replay(mockSession);
        HttpServletRequest mockRequest = createMockServletRequest(mockSession);
        replay(mockRequest);
        return mockRequest;
    }
}
