/**
 * Copyright 2026 Isaac Computer Science
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Unit tests for the {@link LogEventFacade}, focusing on the VIDEO_60_PERCENT_WATCHED event handling
 * (logged-in requirement, payload validation and per-user/per-video deduplication).
 */
class LogEventFacadeTest {

  private static final String VIDEO_EVENT_TYPE = "VIDEO_60_PERCENT_WATCHED";
  private static final long REGISTERED_USER_ID = 5L;
  private static final String REGISTERED_USER_ID_STRING = "5";
  private static final String VIDEO_ID = "abc123";

  private PropertiesLoader properties;
  private ILogManager logManager;
  private IMisuseMonitor misuseMonitor;
  private UserAccountManager userManager;
  private HttpServletRequest request;
  private LogEventFacade facade;

  @BeforeEach
  void setUp() {
    this.properties = createMock(PropertiesLoader.class);
    this.logManager = createMock(ILogManager.class);
    this.misuseMonitor = createMock(IMisuseMonitor.class);
    this.userManager = createMock(UserAccountManager.class);
    this.request = createMock(HttpServletRequest.class);
    replay(this.properties);
    this.facade = new LogEventFacade(this.properties, this.logManager, this.misuseMonitor, this.userManager);
  }

  private static RegisteredUserDTO registeredUser() {
    RegisteredUserDTO user = new RegisteredUserDTO();
    user.setId(REGISTERED_USER_ID);
    return user;
  }

  private static Map<String, Object> validVideoEventPayload() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", VIDEO_EVENT_TYPE);
    payload.put("videoId", VIDEO_ID);
    payload.put("videoUrl", "https://www.youtube.com/watch?v=abc123");
    payload.put("pageId", "some_page");
    payload.put("videoDurationSeconds", 120);
    payload.put("watchedSeconds", 80);
    payload.put("watchPercent", 0.67);
    return payload;
  }

  @Test
  void postLog_validVideoEventForNewUserVideo_isPersistedAndReturnsOk() throws Exception {
    expect(userManager.getCurrentUser(request)).andReturn(registeredUser()).anyTimes();
    expect(request.getContentLength()).andReturn(256).anyTimes();
    misuseMonitor.notifyEvent(eq(REGISTERED_USER_ID_STRING), anyString(), anyInt());
    expectLastCall();
    expect(logManager.userHasLoggedEventWithDetail(REGISTERED_USER_ID_STRING, VIDEO_EVENT_TYPE, "videoId", VIDEO_ID))
        .andReturn(false);
    logManager.logExternalEvent(anyObject(), eq(request), eq(VIDEO_EVENT_TYPE), anyObject());
    expectLastCall();
    replay(userManager, request, misuseMonitor, logManager);

    try (Response response = facade.postLog(request, validVideoEventPayload())) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
    verify(userManager, misuseMonitor, logManager);
  }

  @Test
  void postLog_videoEventFromAnonymousUser_returnsUnauthorized() throws Exception {
    expect(userManager.getCurrentUser(request)).andReturn(new AnonymousUserDTO("anon-session")).anyTimes();
    expect(request.getContentLength()).andReturn(256).anyTimes();
    // Rate limiting still applies to anonymous requests, but no event should be persisted.
    misuseMonitor.notifyEvent(eq("anon-session"), anyString(), anyInt());
    expectLastCall();
    replay(userManager, request, misuseMonitor, logManager);

    try (Response response = facade.postLog(request, validVideoEventPayload())) {
      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
    // The log manager must not be touched for a rejected anonymous request.
    verify(misuseMonitor, logManager);
  }

  @Test
  void postLog_videoEventWithMissingVideoId_returnsBadRequest() throws Exception {
    Map<String, Object> payload = validVideoEventPayload();
    payload.remove("videoId");
    assertVideoEventRejectedAsBadRequest(payload);
  }

  @Test
  void postLog_videoEventWithBlankVideoId_returnsBadRequest() throws Exception {
    Map<String, Object> payload = validVideoEventPayload();
    payload.put("videoId", "   ");
    assertVideoEventRejectedAsBadRequest(payload);
  }

  @Test
  void postLog_videoEventWithNonPositiveDuration_returnsBadRequest() throws Exception {
    Map<String, Object> payload = validVideoEventPayload();
    payload.put("videoDurationSeconds", 0);
    assertVideoEventRejectedAsBadRequest(payload);
  }

  @Test
  void postLog_videoEventWithNegativeWatchedSeconds_returnsBadRequest() throws Exception {
    Map<String, Object> payload = validVideoEventPayload();
    payload.put("watchedSeconds", -1);
    assertVideoEventRejectedAsBadRequest(payload);
  }

  @Test
  void postLog_videoEventBelowWatchThreshold_returnsBadRequest() throws Exception {
    Map<String, Object> payload = validVideoEventPayload();
    payload.put("watchPercent", 0.59);
    assertVideoEventRejectedAsBadRequest(payload);
  }

  @Test
  void postLog_duplicateVideoEventForUserAndVideo_isSkippedButStillCountsTowardMisuse() throws Exception {
    expect(userManager.getCurrentUser(request)).andReturn(registeredUser()).anyTimes();
    expect(request.getContentLength()).andReturn(256).anyTimes();
    // A duplicate request must still be rate-limited so it cannot be used to bypass the misuse monitor.
    misuseMonitor.notifyEvent(eq(REGISTERED_USER_ID_STRING), anyString(), anyInt());
    expectLastCall();
    expect(logManager.userHasLoggedEventWithDetail(REGISTERED_USER_ID_STRING, VIDEO_EVENT_TYPE, "videoId", VIDEO_ID))
        .andReturn(true);
    // logExternalEvent must NOT be called when the event is a duplicate.
    replay(userManager, request, misuseMonitor, logManager);

    try (Response response = facade.postLog(request, validVideoEventPayload())) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
    // verify() confirms notifyEvent was invoked (duplicate counted) and logExternalEvent was not.
    verify(misuseMonitor, logManager);
  }

  /**
   * Helper asserting that an invalid video event payload is rejected with 400 before any dedup/persist work happens.
   */
  private void assertVideoEventRejectedAsBadRequest(final Map<String, Object> payload) throws Exception {
    expect(userManager.getCurrentUser(request)).andReturn(registeredUser()).anyTimes();
    expect(request.getContentLength()).andReturn(256).anyTimes();
    // Rate limiting runs first; validation then fails before any dedup/persistence work hits the log manager.
    misuseMonitor.notifyEvent(eq(REGISTERED_USER_ID_STRING), anyString(), anyInt());
    expectLastCall();
    replay(userManager, request, misuseMonitor, logManager);

    try (Response response = facade.postLog(request, payload)) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    verify(misuseMonitor, logManager);
  }
}
