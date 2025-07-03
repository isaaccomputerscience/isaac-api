package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_FEEDBACK_DAYS_AGO;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_SECOND_FEEDBACK_HOURS;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_MAX_WINDOW_SIZE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;
import java.util.Arrays;

/**
 * Unit tests for EventNotificationEmailManager.
 */
class EventNotificationEmailManagerTest {

  private GitContentManager mockContentManager;
  private EventBookingManager mockBookingManager;
  private UserAccountManager mockUserAccountManager;
  private EmailManager mockEmailManager;
  private PgScheduledEmailManager mockPgScheduledEmailManager;
  private EventNotificationEmailManager eventNotificationEmailManager;
  private Object[] mockedObjects;

  @BeforeEach
  void setUp() {
    mockContentManager = createMock(GitContentManager.class);
    mockBookingManager = createMock(EventBookingManager.class);
    mockUserAccountManager = createMock(UserAccountManager.class);
    mockEmailManager = createMock(EmailManager.class);
    mockPgScheduledEmailManager = createMock(PgScheduledEmailManager.class);

    mockedObjects = new Object[]{
        mockContentManager, mockBookingManager, mockUserAccountManager,
        mockEmailManager, mockPgScheduledEmailManager
    };

    eventNotificationEmailManager = new EventNotificationEmailManager(
        mockContentManager, mockBookingManager, mockUserAccountManager,
        mockEmailManager, mockPgScheduledEmailManager
    );
  }

  @Nested
  class SendFeedbackEmails {

    @Test
    void sendFeedbackEmails_EventInFirstWindowWithPostResources_SendsFeedbackEmail() throws Exception {
      // Arrange
      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS); // 25 hours ago
      IsaacEventPageDTO event = createTestEvent("event1", eventDate, null, EventStatus.FULLY_BOOKED);
      event.setPostResources(ImmutableList.of(createExternalReference("resource1")));

      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 2);

      setupContentManagerExpectations(Collections.singletonList(event));
      setupBookingManagerExpectations(event.getId(), bookings);
      setupEmailSendingExpectations(event, "post", "event_feedback", 2);

      replay(mockedObjects);

      // Act
      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());

      // Assert
      verify(mockedObjects);
    }

//    @Test
//    void sendFeedbackEmails_EventInSecondWindowWithPostResources_SendsFeedbackEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(97, ChronoUnit.HOURS); // 97 hours ago
//      IsaacEventPageDTO event = createTestEvent("event2", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource2")));
//
//      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 1);
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      setupBookingManagerExpectations(event.getId(), bookings);
//      setupEmailSendingExpectations(event, "post_second_trigger", "event_feedback", 1);
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EventWithEndDateInFirstWindow_SendsFeedbackEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(30, ChronoUnit.HOURS);
//      Instant endDate = Instant.now().minus(25, ChronoUnit.HOURS); // 25 hours ago
//      IsaacEventPageDTO event = createTestEvent("event3", eventDate, endDate, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource3")));
//
//      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 3);
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      setupBookingManagerExpectations(event.getId(), bookings);
//      setupEmailSendingExpectations(event, "post", "event_feedback", 3);
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EventWithSurveyUrlAndTitle_SendsSurveyEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event4", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setEventSurvey("https://survey.com");
//      event.setEventSurveyTitle("Event Survey");
//
//      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 2);
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      setupBookingManagerExpectations(event.getId(), bookings);
//      setupEmailSendingExpectations(event, "survey", "event_feedback", 2);
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EventWithSurveyUrlButNoTitle_SendsSurveyEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event5", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setEventSurvey("https://survey.com");
//      event.setEventSurveyTitle(null); // Backwards compatibility
//
//      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 1);
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      setupBookingManagerExpectations(event.getId(), bookings);
//      setupEmailSendingExpectations(event, "survey", "event_feedback", 1);
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EventWithSurveyUrlAndEmptyTitle_DoesNotSendSurveyEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event6", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setEventSurvey("https://survey.com");
//      event.setEventSurveyTitle(""); // Empty title should prevent survey email
//
//      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 1);
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      setupBookingManagerExpectations(event.getId(), bookings);
//      // No email expectations since survey should not be sent
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_CancelledEvent_DoesNotSendEmails() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event7", eventDate, null, EventStatus.CANCELLED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource7")));
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      // No booking manager or email expectations since cancelled events should be skipped
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EventTooRecent_DoesNotSendEmails() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(12, ChronoUnit.HOURS); // Too recent
//      IsaacEventPageDTO event = createTestEvent("event8", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource8")));
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      // No booking manager or email expectations since event is too recent
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EventTooOld_DoesNotSendEmails() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(5, ChronoUnit.DAYS); // Too old
//      IsaacEventPageDTO event = createTestEvent("event9", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource9")));
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      // No booking manager or email expectations since event is too old
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EventInSecondWindowWithSurvey_SendsSurveyEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(97, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event10", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setEventSurvey("https://survey.com");
//      event.setEventSurveyTitle("Event Survey");
//
//      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 1);
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      setupBookingManagerExpectations(event.getId(), bookings);
//      setupEmailSendingExpectations(event, "survey_second_trigger", "event_feedback", 1);
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EventWithBothPostResourcesAndSurvey_SendsBothEmails() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event11", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource11")));
//      event.setEventSurvey("https://survey.com");
//      event.setEventSurveyTitle("Event Survey");
//
//      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 2);
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      setupBookingManagerExpectations(event.getId(), bookings);
//
//      // Expect both post resources and survey emails
//      expect(mockPgScheduledEmailManager.commitToSchedulingEmail("event11@post")).andReturn(true);
//      expect(mockPgScheduledEmailManager.commitToSchedulingEmail("event11@survey")).andReturn(true);
//
//      // Post resources email
//      expect(mockEmailManager.getEmailTemplateDTO("event_feedback")).andReturn(createEmailTemplate()).times(2);
//      mockEmailManager.sendTemplatedEmailToUser(anyObject(), anyObject(), anyObject(), eq(EmailType.SYSTEM));
//      expectLastCall().times(4); // 2 users * 2 email types
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_ContentManagerException_LogsErrorAndContinues() throws Exception {
//      // Arrange
//      expect(mockContentManager.findByFieldNames(anyObject(), eq(0), eq(DEFAULT_MAX_WINDOW_SIZE), anyObject(), anyObject()))
//          .andThrow(new ContentManagerException("Test exception"));
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_SegueDatabaseException_LogsErrorAndContinues() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event12", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource12")));
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      expect(mockBookingManager.adminGetBookingsByEventId(event.getId()))
//          .andThrow(new SegueDatabaseException("Test exception"));
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_NoUserException_LogsErrorAndContinues() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event13", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource13")));
//
//      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 1);
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      setupBookingManagerExpectations(event.getId(), bookings);
//      expect(mockPgScheduledEmailManager.commitToSchedulingEmail("event13@post")).andReturn(true);
//      expect(mockEmailManager.getEmailTemplateDTO("event_feedback")).andReturn(createEmailTemplate());
//      expect(mockUserAccountManager.getUserDTOById(1L)).andThrow(new NoUserException("User not found"));
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EmailAlreadyScheduled_DoesNotSendEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event14", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of(createExternalReference("resource14")));
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      expect(mockPgScheduledEmailManager.commitToSchedulingEmail("event14@post")).andReturn(false);
//      // No email expectations since email was already scheduled
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EmptyPostResources_DoesNotSendPostResourcesEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event15", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(ImmutableList.of()); // Empty list
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      // No booking manager or email expectations since no post resources
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_NullPostResources_DoesNotSendPostResourcesEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event16", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(null); // Null post resources
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      // No booking manager or email expectations since null post resources
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_PostResourcesWithNullElement_DoesNotSendPostResourcesEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event17", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setPostResources(Arrays.asList(null, createExternalReference("resource17"))); // Contains null
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      // No booking manager or email expectations since post resources contains null
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_EmptySurveyUrl_DoesNotSendSurveyEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event18", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setEventSurvey(""); // Empty survey URL
//      event.setEventSurveyTitle("Event Survey");
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      // No booking manager or email expectations since empty survey URL
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_NullSurveyUrl_DoesNotSendSurveyEmail() throws Exception {
//      // Arrange
//      Instant eventDate = Instant.now().minus(25, ChronoUnit.HOURS);
//      IsaacEventPageDTO event = createTestEvent("event19", eventDate, null, EventStatus.FULLY_BOOKED);
//      event.setEventSurvey(null); // Null survey URL
//      event.setEventSurveyTitle("Event Survey");
//
//      setupContentManagerExpectations(Collections.singletonList(event));
//      // No booking manager or email expectations since null survey URL
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
//
//    @Test
//    void sendFeedbackEmails_MultipleEvents_ProcessesAllEvents() throws Exception {
//      // Arrange
//      Instant eventDate1 = Instant.now().minus(25, ChronoUnit.HOURS);
//      Instant eventDate2 = Instant.now().minus(97, ChronoUnit.HOURS);
//
//      IsaacEventPageDTO event1 = createTestEvent("event20", eventDate1, null, EventStatus.FULLY_BOOKED);
//      event1.setPostResources(ImmutableList.of(createExternalReference("resource20")));
//
//      IsaacEventPageDTO event2 = createTestEvent("event21", eventDate2, null, EventStatus.FULLY_BOOKED);
//      event2.setEventSurvey("https://survey.com");
//      event2.setEventSurveyTitle("Event Survey");
//
//      List<DetailedEventBookingDTO> bookings1 = createAttendedBookings(event1.getId(), 1);
//      List<DetailedEventBookingDTO> bookings2 = createAttendedBookings(event2.getId(), 2);
//
//      setupContentManagerExpectations(ImmutableList.of(event1, event2));
//      setupBookingManagerExpectations(event1.getId(), bookings1);
//      setupBookingManagerExpectations(event2.getId(), bookings2);
//      setupEmailSendingExpectations(event1, "post", "event_feedback", 1);
//      setupEmailSendingExpectations(event2, "survey_second_trigger", "event_feedback", 2);
//
//      replay(mockedObjects);
//
//      // Act
//      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());
//
//      // Assert
//      verify(mockedObjects);
//    }
  }

  // Helper methods

  private IsaacEventPageDTO createTestEvent(String id, Instant date, Instant endDate, EventStatus status) {
    IsaacEventPageDTO event = new IsaacEventPageDTO();
    event.setId(id);
    event.setDate(date);
    event.setEndDate(endDate);
    event.setEventStatus(status);
    event.setType(EVENT_TYPE);
    return event;
  }

  private ExternalReference createExternalReference(String title) {
    ExternalReference ref = new ExternalReference();
    ref.setTitle(title);
    ref.setUrl("https://example.com/" + title);
    return ref;
  }

  private List<DetailedEventBookingDTO> createAttendedBookings(String eventId, int count) {
    ImmutableList.Builder<DetailedEventBookingDTO> builder = ImmutableList.builder();
    for (int i = 1; i <= count; i++) {
      UserSummaryWithEmailAddressDTO user = new UserSummaryWithEmailAddressDTO();
      user.setId((long) i);
      user.setGivenName("User" + i);
      user.setFamilyName("Test");
      user.setEmail("user" + i + "@test.com");

      DetailedEventBookingDTO booking = new DetailedEventBookingDTO();
      booking.setUserBooked(user);
      booking.setBookingStatus(BookingStatus.ATTENDED);
      booking.setEventId(eventId);

      builder.add(booking);
    }
    return builder.build();
  }

  private void setupContentManagerExpectations(List<IsaacEventPageDTO> events) throws ContentManagerException {
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
    fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));

    Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);

    Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime eventFeedbackThresholdDate = now.minusDays(EMAIL_EVENT_FEEDBACK_DAYS_AGO);
    DateRangeFilterInstruction eventsWithinFeedbackDateRange = new DateRangeFilterInstruction(
        eventFeedbackThresholdDate.toInstant(), Instant.now());
    filterInstructions.put(DATE_FIELDNAME, eventsWithinFeedbackDateRange);

    ResultsWrapper<ContentDTO> resultsWrapper = new ResultsWrapper<>((List<ContentDTO>) (List<?>) events, (long) events.size());

    expect(mockContentManager.findByFieldNames(
        anyObject(),
        eq(0),
        eq(DEFAULT_MAX_WINDOW_SIZE),
        anyObject(),
        anyObject()
    )).andReturn(resultsWrapper);
  }

  private void setupBookingManagerExpectations(String eventId, List<DetailedEventBookingDTO> bookings)
      throws SegueDatabaseException {
    expect(mockBookingManager.adminGetBookingsByEventId(eventId)).andReturn(bookings);
  }

  private void setupEmailSendingExpectations(IsaacEventPageDTO event, String emailKeyPostfix,
                                             String templateId, int userCount) throws Exception {
    String emailKey = event.getId() + "@" + emailKeyPostfix;
    expect(mockPgScheduledEmailManager.commitToSchedulingEmail(emailKey)).andReturn(true);
    expect(mockEmailManager.getEmailTemplateDTO(templateId)).andReturn(createEmailTemplate());

    for (int i = 0; i < userCount; i++) {
      RegisteredUserDTO user = new RegisteredUserDTO();
      user.setId((long) (i + 1));
      user.setGivenName("User" + (i + 1));
      user.setFamilyName("Test");
      user.setEmail("user" + (i + 1) + "@test.com");

      expect(mockUserAccountManager.getUserDTOById((long) (i + 1))).andReturn(user);
      mockEmailManager.sendTemplatedEmailToUser(eq(user), anyObject(), anyObject(), eq(EmailType.SYSTEM));
      expectLastCall();
    }
  }

  private uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO createEmailTemplate() {
    uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO template =
        new uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO();
    template.setId("test-template");
    template.setSubject("Test Subject");
    template.setHtmlContent("<p>Test content</p>");
    template.setPlainTextContent("Test content");
    return template;
  }
}
