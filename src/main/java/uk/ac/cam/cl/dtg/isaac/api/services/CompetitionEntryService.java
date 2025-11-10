package uk.ac.cam.cl.dtg.isaac.api.services;


import static uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager.STUDENTS_LIST;
import static uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager.STUDENTS_LIST_HTML;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

import com.google.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.CompetitionEntryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Service for handling competition entry submissions and confirmations.
 * This service is responsible for sending automated confirmation emails
 * to teachers immediately after they submit a competition entry.
 * The email includes details of the submission such as project title,
 * project link, group name, and list of students.
 *
 */
public class CompetitionEntryService {
  private static final Logger log = LoggerFactory.getLogger(CompetitionEntryService.class);

  private static final String EMAIL_TEMPLATE_ID = "email_competition_entry_confirmation";

  private final EmailManager emailManager;
  private final UserAccountManager userAccountManager;
  private final PropertiesLoader propertiesLoader;

  @Inject
  public CompetitionEntryService(
      final EmailManager emailManager,
      final UserAccountManager userAccountManager,
      PropertiesLoader propertiesLoader) {
    this.emailManager = emailManager;
    this.userAccountManager = userAccountManager;
    this.propertiesLoader = propertiesLoader;
  }

  /**
   * Send confirmation email to teacher after competition entry submission.
   *
   * @param event         The competition event (must not be null)
   * @param entryDTO      The competition entry DTO containing submission details
   * @param reservingUser The teacher who submitted the entry (must not be null)
   */
  public void sendCompetitionEntryConfirmation(
      final IsaacEventPageDTO event,
      final CompetitionEntryDTO entryDTO,
      final RegisteredUserDTO reservingUser) {

    if (event == null || entryDTO == null || reservingUser == null) {
      log.error("Cannot send competition entry confirmation: null parameter provided");
      return;
    }

    try {
      log.info("Preparing to send competition entry confirmation email for user ID: {}, event: {}",
          reservingUser.getId(), event.getId());

      final Map<String, Object> emailContext = buildEmailContext(event, entryDTO, reservingUser);

      final EmailTemplateDTO emailTemplate = emailManager.getEmailTemplateDTO(EMAIL_TEMPLATE_ID);

      emailManager.sendTemplatedEmailToUser(
          reservingUser,
          emailTemplate,
          emailContext,
          EmailType.SYSTEM
      );

      log.info("Successfully sent competition entry confirmation email to teacher: {} {}, email: {}",
          reservingUser.getGivenName(), reservingUser.getFamilyName(), reservingUser.getEmail());

    } catch (ContentManagerException e) {
      log.error("Failed to send competition entry confirmation: Email template '{}' not found or invalid",
          EMAIL_TEMPLATE_ID, e);
    } catch (SegueDatabaseException e) {
      log.error("Failed to send competition entry confirmation: Database error for user ID {}",
          reservingUser.getId(), e);
    } catch (Exception e) {
      log.error("Unexpected error sending competition entry confirmation email for user ID {}",
          reservingUser.getId(), e);
    }
  }

  /**
   * Build the email context map with all template variables.
   * This creates a map containing all the variables that will be substituted
   * into the email template, including event details and submission data.
   * This method extracts information from the CompetitionEntryDTO and looks up
   * student names from their user IDs.
   *
   * @param event         The competition event
   * @param entryDTO      The competition entry DTO
   * @param reservingUser The teacher submitting the entry
   * @return Map of template variables
   */
  private Map<String, Object> buildEmailContext(
      final IsaacEventPageDTO event,
      final CompetitionEntryDTO entryDTO,
      final RegisteredUserDTO reservingUser) {

    var studentsList = formatStudentsList(entryDTO.getEntrantIds());

    final String projectTitle = entryDTO.getProjectTitle() != null ? entryDTO.getProjectTitle() : "";
    final String projectLink = entryDTO.getSubmissionURL() != null ? entryDTO.getSubmissionURL() : "";
    final String groupName = entryDTO.getGroupName() != null ? entryDTO.getGroupName() : "";

    return Map.of(
        "event", event,
        "givenName", reservingUser.getGivenName(),
        "projectTitle", projectTitle,
        "projectLink", projectLink,
        "groupName", groupName,
        STUDENTS_LIST, studentsList.get(STUDENTS_LIST),
        STUDENTS_LIST_HTML, studentsList.get(STUDENTS_LIST_HTML),
        "contactUsURL", generateEventContactUsURL(reservingUser.getGivenName(),
            reservingUser.getFamilyName(),
            reservingUser.getEmail(),
            "Competition entry - " + entryDTO.getProjectTitle())
    );
  }

  public String generateEventContactUsURL(String givenName, String familyName, String email, String subject) {
    List<String> params = new ArrayList<>();

    if (subject != null) {
      params.add("subject=" + URLEncoder.encode(subject, StandardCharsets.UTF_8));
    }
    if (email != null) {
      params.add("email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
    }
    if (givenName != null) {
      params.add("firstName=" + URLEncoder.encode(givenName, StandardCharsets.UTF_8));
    }
    if (familyName != null) {
      params.add("lastName=" + URLEncoder.encode(familyName, StandardCharsets.UTF_8));
    }

    String baseUrl = String.format("https://%s/contact", propertiesLoader.getProperty(HOST_NAME));

    if (!params.isEmpty()) {
      return baseUrl + "?" + String.join("&", params);
    }

    return baseUrl;
  }

  /**
   * Format the list of students as an HTML bullet list for the email.
   * Returns an HTML unordered list (&lt;ul&gt;) with each student as a list item (&lt;li&gt;).
   * If a user cannot be found or there's an error, that student will be
   * skipped with a log message.
   *
   * @param entrantIds List of student user IDs
   * @return HTML formatted bullet list of students
   */
  private Map<String, String> formatStudentsList(final List<Long> entrantIds) {
    if (entrantIds == null || entrantIds.isEmpty()) {
      return Map.of();
    }

    StringBuilder htmlString = new StringBuilder();
    StringBuilder plainTextString = new StringBuilder();
    htmlString.append("<ul>");

    for (Long userId : entrantIds) {
      try {
        RegisteredUserDTO student = userAccountManager.getUserDTOById(userId);

        String firstName = student.getGivenName() != null ? student.getGivenName() : "";
        String lastName = student.getFamilyName() != null ? student.getFamilyName() : "";

        String userFullName = String.format("%s %s", firstName, lastName);
        htmlString.append(String.format("<li>%s</li>", userFullName));
        plainTextString.append(String.format("- %s%n", userFullName));

      } catch (NoUserException e) {
        log.error("Could not find student with ID {} for competition entry confirmation email", userId);
      } catch (Exception e) {
        log.error("Error looking up student ID {} for email", userId, e);
      }
    }

    htmlString.append("</ul>");

    // If only the opening and closing tags exist, return fallback message
    String result = htmlString.toString();
    if (result.equals("<ul></ul>")) {
      return Map.of();
    }

    return Map.of(STUDENTS_LIST_HTML, result, STUDENTS_LIST, plainTextString.toString());
  }
}