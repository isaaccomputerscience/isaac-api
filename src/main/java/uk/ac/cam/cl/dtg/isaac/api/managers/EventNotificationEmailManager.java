package uk.ac.cam.cl.dtg.isaac.api.managers;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_FEEDBACK_DAYS_AGO;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_REMINDER_DAYS_AHEAD;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_SECOND_FEEDBACK_HOURS;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_MAX_WINDOW_SIZE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
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

public class EventNotificationEmailManager {
  private static final Logger log = LoggerFactory.getLogger(EventNotificationEmailManager.class);

  private final GitContentManager contentManager;
  private final EventBookingManager bookingManager;
  private final UserAccountManager userAccountManager;
  private final EmailManager emailManager;
  private final PgScheduledEmailManager pgScheduledEmailManager;


  /**
   * This class is required by quartz and must be executable by any instance of the segue api relying only on the
   * jobdata context provided.
   *
   * @param contentManager          - for retrieving content
   * @param bookingManager          - Instance of Booking Manager
   * @param userAccountManager      - Instance of User Account Manager, for retrieving users
   * @param emailManager            - for constructing and sending emails
   * @param pgScheduledEmailManager - for scheduling the sending of emails
   */
  @Inject
  public EventNotificationEmailManager(
      final GitContentManager contentManager,
      final EventBookingManager bookingManager,
      final UserAccountManager userAccountManager,
      final EmailManager emailManager,
      final PgScheduledEmailManager pgScheduledEmailManager
  ) {
    this.contentManager = contentManager;
    this.bookingManager = bookingManager;
    this.userAccountManager = userAccountManager;
    this.emailManager = emailManager;
    this.pgScheduledEmailManager = pgScheduledEmailManager;
  }

  public void sendBookingStatusFilteredEmailForEvent(final IsaacEventPageDTO event, final String templateId,
                                                     final List<BookingStatus> bookingStatuses)
      throws SegueDatabaseException {
    List<DetailedEventBookingDTO> eventBookings = bookingManager.adminGetBookingsByEventId(event.getId());
    List<Long> ids = eventBookings.stream()
        .filter(detailedEventBookingDTO -> bookingStatuses == null || bookingStatuses.contains(
            detailedEventBookingDTO.getBookingStatus()))
        .map(DetailedEventBookingDTO::getUserBooked)
        .map(UserSummaryDTO::getId)
        .distinct().collect(Collectors.toList());
    for (Long id : ids) {
      try {
        RegisteredUserDTO user = userAccountManager.getUserDTOById(id);
        emailManager.sendTemplatedEmailToUser(user,
            emailManager.getEmailTemplateDTO(templateId),
            new ImmutableMap.Builder<String, Object>()
                .put("event.emailEventDetails",
                    event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                .put("event", event)
                .build(),
            EmailType.SYSTEM);
        log.debug("Sent email to user: {} {}, at: {}", user.getGivenName(), user.getFamilyName(), user.getEmail());
      } catch (NoUserException e) {
        log.error(String.format("No user found with ID: %s", id));
      } catch (ContentManagerException e) {
        log.error("Failed to add the scheduled email sent time: ", e);
      }
    }
  }

  private void commitAndSendReminderEmail(IsaacEventPageDTO event, String emailKeyPostfix, String templateId)
      throws SegueDatabaseException {
    String emailKey = String.format("%s@%s", event.getId(), emailKeyPostfix);
    /*
    Confirmed and Attended statuses are both included for pre-event emails.
    Pre-event emails include the attended status in case the events team have pre-emptively marked someone as attended.
     */
    List<BookingStatus> bookingStatuses = Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.ATTENDED);
    if (pgScheduledEmailManager.commitToSchedulingEmail(emailKey)) {
      this.sendBookingStatusFilteredEmailForEvent(event, templateId, bookingStatuses);
    }
  }

  public void sendReminderEmails() {
    // Magic number
    Integer startIndex = 0;
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
    Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
    fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));
    sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime eventReminderThresholdDate = now.plusDays(EMAIL_EVENT_REMINDER_DAYS_AHEAD);
    Instant endOfToday = now.with(LocalTime.MAX).toInstant();
    DateRangeFilterInstruction eventsWithinReminderDateRange =
        new DateRangeFilterInstruction(Instant.now(), eventReminderThresholdDate.toInstant());
    filterInstructions.put(DATE_FIELDNAME, eventsWithinReminderDateRange);

    try {
      ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
          ContentService.generateDefaultFieldToMatch(fieldsToMatch), startIndex, DEFAULT_MAX_WINDOW_SIZE,
          sortInstructions,
          filterInstructions);
      for (ContentDTO contentResult : findByFieldNames.getResults()) {
        if (contentResult instanceof IsaacEventPageDTO) {
          IsaacEventPageDTO event = (IsaacEventPageDTO) contentResult;
          // Skip sending emails for cancelled events
          if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
            continue;
          }
          if (event.getDate().isAfter(endOfToday)) {
            commitAndSendReminderEmail(event, "pre", "event_reminder");
          } else {
            commitAndSendReminderEmail(event, "presameday", "event_reminder_same_day");
          }
        }
      }
    } catch (ContentManagerException | SegueDatabaseException e) {
      log.error("Failed to send scheduled event reminder emails: ", e);
    }
  }

  private void commitAndSendFeedbackEmail(IsaacEventPageDTO event, String emailKeyPostfix, String templateId)
      throws SegueDatabaseException {
    String emailKey = String.format("%s@%s", event.getId(), emailKeyPostfix);
    /*
    Attended statuses included for post-event emails.
     */
    List<BookingStatus> bookingStatuses = List.of(BookingStatus.ATTENDED);
    if (pgScheduledEmailManager.commitToSchedulingEmail(emailKey)) {
      this.sendBookingStatusFilteredEmailForEvent(event, templateId, bookingStatuses);
    }
  }

  public void sendFeedbackEmails() {
    Integer startIndex = 0;
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
    Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
    fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));
    sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime eventFeedbackThresholdDate = now.minusDays(EMAIL_EVENT_FEEDBACK_DAYS_AGO);

    DateRangeFilterInstruction eventsWithinFeedbackDateRange = new DateRangeFilterInstruction(
        eventFeedbackThresholdDate.toInstant(), Instant.now());
    filterInstructions.put(DATE_FIELDNAME, eventsWithinFeedbackDateRange);

    try {
      ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
          ContentService.generateDefaultFieldToMatch(fieldsToMatch), startIndex, DEFAULT_MAX_WINDOW_SIZE,
          sortInstructions,
          filterInstructions);
      for (ContentDTO contentResult : findByFieldNames.getResults()) {
        if (contentResult instanceof IsaacEventPageDTO event) {
          // Skip sending emails for cancelled events
          if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
            continue;
          }

          // Event end date (if present) is past, else event date is past
          boolean endDatePast = event.getEndDate() != null && event.getEndDate().isBefore(Instant.now());
          boolean noEndDateAndStartDatePast = event.getEndDate() == null && event.getDate().isBefore(Instant.now());

          if (endDatePast || noEndDateAndStartDatePast) {
            Instant referenceDate = event.getEndDate() != null ? event.getEndDate() : event.getDate();

            // New logic for sending survey email
            String surveyUrl = event.getEventSurvey();
            String surveyTitle = event.getEventSurveyTitle();
            // Event end date (if present) is yesterday or before, else event date is yesterday, or before
            // We want to send the event_feedback email 24 hours after the event
            boolean endDateYesterday =
                event.getEndDate() != null && event.getEndDate().isBefore(Instant.now().minus(1, ChronoUnit.DAYS));

            boolean noEndDateAndStartDateYesterday =
                event.getEndDate() == null && event.getDate().isBefore(Instant.now().minus(1, ChronoUnit.DAYS));

            if (endDateYesterday || noEndDateAndStartDateYesterday) {
              List<ExternalReference> postResources = event.getPostResources();

              boolean postResourcesPresent =
                  postResources != null && !postResources.isEmpty() && !postResources.contains(null);

              boolean eventSurveyTitleUrlPresent = (surveyUrl != null && !surveyUrl.isEmpty())
                  && (surveyTitle == null || !surveyTitle.isEmpty());

              if (postResourcesPresent || eventSurveyTitleUrlPresent) {
                commitAndSendFeedbackEmail(event, "post", "event_feedback");
              }
            }
          }
        }
      }
    } catch (ContentManagerException | SegueDatabaseException e) {
      log.error("Failed to send scheduled event feedback emails: ", e);
    }
  }
}
