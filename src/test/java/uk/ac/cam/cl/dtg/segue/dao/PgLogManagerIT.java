package uk.ac.cam.cl.dtg.segue.dao;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.IsaacIntegrationTest;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

/**
 * Integration tests for {@link PgLogManager#userHasLoggedEventWithDetail}, exercising the JSONB lookup that backs
 * VIDEO_60_PERCENT_WATCHED deduplication against a real Postgres instance.
 */
class PgLogManagerIT extends IsaacIntegrationTest {

  private static final String VIDEO_EVENT_TYPE = "VIDEO_60_PERCENT_WATCHED";
  private static final String VIDEO_ID_FIELD = "videoId";

  private PgLogManager pgLogManager;

  @BeforeEach
  public void setUp() {
    // A real PgLogManager wired to the test Postgres container, with logging enabled so events are persisted.
    pgLogManager = new PgLogManager(postgresSqlDb, new ObjectMapper(), true);
  }

  private static RegisteredUserDTO userWithId(final long id) {
    RegisteredUserDTO user = new RegisteredUserDTO();
    user.setId(id);
    return user;
  }

  private void persistVideoEvent(final long userId, final String videoId) {
    HttpServletRequest request = createNiceMock(HttpServletRequest.class);
    replay(request);
    Map<String, Object> eventDetails = new HashMap<>();
    eventDetails.put(VIDEO_ID_FIELD, videoId);
    eventDetails.put("videoUrl", "https://www.youtube.com/watch?v=" + videoId);
    eventDetails.put("videoDurationSeconds", 120);
    eventDetails.put("watchedSeconds", 80);
    eventDetails.put("watchPercent", 0.67);
    pgLogManager.logExternalEvent(userWithId(userId), request, VIDEO_EVENT_TYPE, eventDetails);
  }

  @Test
  void userHasLoggedEventWithDetail_matchesPersistedUserAndVideo() throws Exception {
    String videoId = "it-video-match-1";
    persistVideoEvent(TEST_STUDENT_ID, videoId);

    assertTrue(pgLogManager.userHasLoggedEventWithDetail(
        String.valueOf(TEST_STUDENT_ID), VIDEO_EVENT_TYPE, VIDEO_ID_FIELD, videoId));
  }

  @Test
  void userHasLoggedEventWithDetail_doesNotMatchDifferentVideoId() throws Exception {
    String videoId = "it-video-distinct-2";
    persistVideoEvent(TEST_STUDENT_ID, videoId);

    assertFalse(pgLogManager.userHasLoggedEventWithDetail(
        String.valueOf(TEST_STUDENT_ID), VIDEO_EVENT_TYPE, VIDEO_ID_FIELD, "some-other-video"));
  }

  @Test
  void userHasLoggedEventWithDetail_doesNotMatchDifferentUser() throws Exception {
    String videoId = "it-video-user-scoped-3";
    persistVideoEvent(TEST_STUDENT_ID, videoId);

    // The same video watched by the student must not count as watched for the teacher.
    assertFalse(pgLogManager.userHasLoggedEventWithDetail(
        String.valueOf(TEST_TEACHER_ID), VIDEO_EVENT_TYPE, VIDEO_ID_FIELD, videoId));
  }

  @Test
  void userHasLoggedEventWithDetail_doesNotMatchDifferentEventType() throws Exception {
    String videoId = "it-video-type-scoped-4";
    persistVideoEvent(TEST_STUDENT_ID, videoId);

    assertFalse(pgLogManager.userHasLoggedEventWithDetail(
        String.valueOf(TEST_STUDENT_ID), "VIDEO_PLAY", VIDEO_ID_FIELD, videoId));
  }
}