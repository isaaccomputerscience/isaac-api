package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.utils.TestDataFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

@ExtendWith(MockitoExtension.class)
public class EventNotificationEmailManagerTest {

  @Mock
  private GitContentManager contentManager;

  @Mock
  private EventBookingManager eventBookingManager;

  @Mock
  private UserAccountManager userAccountManager;

  @Mock
  private EmailManager emailManager;

  @Mock
  private PgScheduledEmailManager pgScheduledEmailManager;

  @Mock
  private ResultsWrapper<ContentDTO> resultsWrapper;

  private EventNotificationEmailManager eventNotificationEmailManager;

  private IsaacEventPageDTO testEvent;

  private List<ExternalReference> mockPostResources;

  @BeforeEach
  void setUp() {
    eventNotificationEmailManager = new EventNotificationEmailManager(
        contentManager,
        eventBookingManager,
        userAccountManager,
        emailManager,
        pgScheduledEmailManager
    );

    testEvent = new IsaacEventPageDTO();
    testEvent.setId("test-event-1");
    testEvent.setEventStatus(EventStatus.OPEN);
    testEvent.setEventSurvey("https://survey.example.com");
    testEvent.setEventSurveyTitle("Test Survey");

    mockPostResources = List.of(
        new ExternalReference("Resource 1", "https://resource1.com"),
        new ExternalReference("Resource 2", "https://resource2.com")
    );
    testEvent.setPostResources(mockPostResources);
  }

  private void setupMockContentManager(List<IsaacEventPageDTO> events) throws Exception {
    List<ContentDTO> contentResults = new ArrayList<>(events);
    when(resultsWrapper.getResults()).thenReturn(contentResults);
    when(contentManager.findByFieldNames(any(), anyInt(), anyInt(), any(), any()))
        .thenReturn(resultsWrapper);
  }

  @Nested
  @DisplayName("24 Hour Trigger Tests")
  class TwentyFourHourTriggerTests {

    @Test
    @DisplayName("Should send 24-hour email for event ended 25 hours ago")
    void shouldSend24HourEmailForRecentEvent() throws Exception {
      Instant twentyFiveHoursAgo = Instant.now().minus(25, ChronoUnit.HOURS);
      testEvent.setDate(twentyFiveHoursAgo);
      testEvent.setEndDate(null); // Single day event

      setupMockContentManager(Collections.singletonList(testEvent));

      EventNotificationEmailManager spyEventNotificationEmailManager = spy(eventNotificationEmailManager);
      doNothing().when(spyEventNotificationEmailManager)
          .sendBookingStatusFilteredEmailForEvent(any(IsaacEventPageDTO.class), anyString(), any());
      when(pgScheduledEmailManager.commitToSchedulingEmail(anyString())).thenReturn(true);

      spyEventNotificationEmailManager.sendFeedbackEmails();

      verify(pgScheduledEmailManager, atLeastOnce()).commitToSchedulingEmail(eq("test-event-1@post"));
      verify(spyEventNotificationEmailManager, atLeastOnce()).sendBookingStatusFilteredEmailForEvent(
          any(IsaacEventPageDTO.class), eq("event_feedback"), eq(List.of(BookingStatus.ATTENDED)));
    }

    @Test
    @DisplayName("Should send 24-hour email for multi-day event ended 25 hours ago")
    void shouldSend24HourEmailForMultiDayEvent() throws Exception {
      Instant startTime = Instant.now().minus(50, ChronoUnit.HOURS);
      Instant endTime = Instant.now().minus(25, ChronoUnit.HOURS);
      testEvent.setDate(startTime);
      testEvent.setEndDate(endTime);

      setupMockContentManager(Collections.singletonList(testEvent));
      when(pgScheduledEmailManager.commitToSchedulingEmail(anyString())).thenReturn(true);

      eventNotificationEmailManager.sendFeedbackEmails();

      EventNotificationEmailManager spyEventNotificationEmailManager = spy(eventNotificationEmailManager);
      doNothing().when(spyEventNotificationEmailManager)
          .sendBookingStatusFilteredEmailForEvent(any(IsaacEventPageDTO.class), anyString(), any());

      spyEventNotificationEmailManager.sendFeedbackEmails();

      verify(pgScheduledEmailManager, atLeastOnce()).commitToSchedulingEmail(eq("test-event-1@post"));
      verify(spyEventNotificationEmailManager, atLeastOnce()).sendBookingStatusFilteredEmailForEvent(
          any(IsaacEventPageDTO.class), eq("event_feedback"), eq(List.of(BookingStatus.ATTENDED)));
    }

    @Test
    @DisplayName("Should NOT send 24-hour email for event ended less than 24 hours ago")
    void shouldNotSend24HourEmailForTooRecentEvent() throws Exception {
      // Given
      Instant twentyHoursAgo = Instant.now().minus(20, ChronoUnit.HOURS);
      testEvent.setDate(twentyHoursAgo);

      setupMockContentManager(Collections.singletonList(testEvent));

      eventNotificationEmailManager.sendFeedbackEmails();

      EventNotificationEmailManager spyEventNotificationEmailManager = spy(eventNotificationEmailManager);

      spyEventNotificationEmailManager.sendFeedbackEmails();

      verify(pgScheduledEmailManager, never()).commitToSchedulingEmail(eq("test-event-1@post"));
      verify(spyEventNotificationEmailManager, never()).sendBookingStatusFilteredEmailForEvent(
          any(IsaacEventPageDTO.class), eq("event_feedback"), eq(List.of(BookingStatus.ATTENDED)));
    }
  }

  @Nested
  @DisplayName("96 Hour Trigger Tests")
  class NinetySixHourTriggerTests {

    @Test
    @DisplayName("Should send 96-hour email for event ended 100 hours ago")
    void shouldSend96HourEmailForOldEvent() throws Exception {
      Instant hundredHoursAgo = Instant.now().minus(100, ChronoUnit.HOURS);
      testEvent.setDate(hundredHoursAgo);
      testEvent.setEndDate(null);

      setupMockContentManager(Collections.singletonList(testEvent));
      when(pgScheduledEmailManager.commitToSchedulingEmail(anyString())).thenReturn(true);

      eventNotificationEmailManager.sendFeedbackEmails();

      EventNotificationEmailManager spyEventNotificationEmailManager = spy(eventNotificationEmailManager);
      doNothing().when(spyEventNotificationEmailManager)
          .sendBookingStatusFilteredEmailForEvent(any(IsaacEventPageDTO.class), anyString(), any());

      spyEventNotificationEmailManager.sendFeedbackEmails();

      verify(pgScheduledEmailManager, atLeastOnce()).commitToSchedulingEmail(eq("test-event-1@survey96"));
      verify(spyEventNotificationEmailManager, atLeastOnce()).sendBookingStatusFilteredEmailForEvent(
          any(IsaacEventPageDTO.class), eq("event_survey"), eq(List.of(BookingStatus.ATTENDED)));
    }

    @Test
    @DisplayName("Should send 96-hour email for multi-day event ended 100 hours ago")
    void shouldSend96HourEmailForOldMultiDayEvent() throws Exception {
      Instant startTime = Instant.now().minus(120, ChronoUnit.HOURS);
      Instant endTime = Instant.now().minus(100, ChronoUnit.HOURS);
      testEvent.setDate(startTime);
      testEvent.setEndDate(endTime);

      setupMockContentManager(Collections.singletonList(testEvent));

      when(pgScheduledEmailManager.commitToSchedulingEmail(anyString())).thenReturn(true);

      eventNotificationEmailManager.sendFeedbackEmails();

      EventNotificationEmailManager spyEventNotificationEmailManager = spy(eventNotificationEmailManager);
      doNothing().when(spyEventNotificationEmailManager)
          .sendBookingStatusFilteredEmailForEvent(any(IsaacEventPageDTO.class), anyString(), any());

      spyEventNotificationEmailManager.sendFeedbackEmails();

      verify(pgScheduledEmailManager, atLeastOnce()).commitToSchedulingEmail(eq("test-event-1@survey96"));
      verify(spyEventNotificationEmailManager, atLeastOnce()).sendBookingStatusFilteredEmailForEvent(
          any(IsaacEventPageDTO.class), eq("event_survey"), eq(List.of(BookingStatus.ATTENDED)));
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

      @Test
      @DisplayName("Should skip cancelled events")
      void shouldSkipCancelledEvents() throws Exception {
        testEvent.setEventStatus(EventStatus.CANCELLED);
        testEvent.setDate(Instant.now().minus(25, ChronoUnit.HOURS));

        setupMockContentManager(Collections.singletonList(testEvent));

        eventNotificationEmailManager.sendFeedbackEmails();

        verify(pgScheduledEmailManager, never()).commitToSchedulingEmail(any());
      }

      @Test
      @DisplayName("Should NOT send email when no post resources and no survey")
      void shouldNotSendEmailWithoutResourcesOrSurvey() throws Exception {
        testEvent.setPostResources(null);
        testEvent.setEventSurvey(null);
        testEvent.setEventSurveyTitle(null);
        testEvent.setDate(Instant.now().minus(25, ChronoUnit.HOURS));

        setupMockContentManager(Collections.singletonList(testEvent));
        eventNotificationEmailManager.sendFeedbackEmails();

        verify(pgScheduledEmailManager, never()).commitToSchedulingEmail(any());
      }

      @Test
      @DisplayName("Should handle multiple events with different timings correctly")
      void shouldHandleMultipleEventsCorrectly() throws Exception {
        IsaacEventPageDTO event24h = TestDataFactory.createTestEvent("event-24h", 25, mockPostResources);
        IsaacEventPageDTO event96h = TestDataFactory.createTestEvent("event-96h", 100, mockPostResources);
        IsaacEventPageDTO eventTooRecent = TestDataFactory.createTestEvent("event-recent", 20, mockPostResources);

        setupMockContentManager(Arrays.asList(event24h, event96h, eventTooRecent));

        when(pgScheduledEmailManager.commitToSchedulingEmail(anyString())).thenReturn(true);

        eventNotificationEmailManager.sendFeedbackEmails();

        EventNotificationEmailManager spyEventNotificationEmailManager = spy(eventNotificationEmailManager);
        doNothing().when(spyEventNotificationEmailManager)
            .sendBookingStatusFilteredEmailForEvent(any(IsaacEventPageDTO.class), anyString(), any());

        spyEventNotificationEmailManager.sendFeedbackEmails();

        verify(pgScheduledEmailManager, atLeastOnce()).commitToSchedulingEmail(eq("event-24h@post"));
        verify(spyEventNotificationEmailManager, atLeastOnce()).sendBookingStatusFilteredEmailForEvent(
            any(IsaacEventPageDTO.class), eq("event_feedback"), eq(List.of(BookingStatus.ATTENDED)));

        verify(pgScheduledEmailManager, atLeastOnce()).commitToSchedulingEmail(eq("event-96h@survey96"));
        verify(spyEventNotificationEmailManager, atLeastOnce()).sendBookingStatusFilteredEmailForEvent(
            any(IsaacEventPageDTO.class), eq("event_survey"), eq(List.of(BookingStatus.ATTENDED)));

      }
    }
  }
}
