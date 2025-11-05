package uk.ac.cam.cl.dtg.isaac.api.services;


import com.google.inject.Inject;
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
  private static final String CONTACT_US_EMAIL = "contact@isaaccomputerscience.org";

  private final EmailManager emailManager;
  private final UserAccountManager userAccountManager;

  @Inject
  public CompetitionEntryService(
      final EmailManager emailManager,
      final UserAccountManager userAccountManager) {
    this.emailManager = emailManager;
    this.userAccountManager = userAccountManager;
  }

  /**
   * Send confirmation email to teacher after competition entry submission.
   * ! Only logs the error !
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
   * @param event         The competition event
   * @param entryDTO      The competition entry DTO
   * @param reservingUser The teacher submitting the entry
   * @return Map of template variables
   */
  private Map<String, Object> buildEmailContext(
      final IsaacEventPageDTO event,
      final CompetitionEntryDTO entryDTO,
      final RegisteredUserDTO reservingUser) {

    final String studentsList = formatStudentsList(entryDTO.getEntrantIds());

    final String projectTitle = entryDTO.getProjectTitle() != null ? entryDTO.getProjectTitle() : "";
    final String projectLink = entryDTO.getSubmissionURL() != null ? entryDTO.getSubmissionURL() : "";
    final String groupName = entryDTO.getGroupName() != null ? entryDTO.getGroupName() : "";

    return Map.of(
        "event", event,
        "givenName", reservingUser.getGivenName(),
        "projectTitle", projectTitle,
        "projectLink", projectLink,
        "groupName", groupName,
        "studentsList", studentsList,
        "contactUsURL", CONTACT_US_EMAIL
    );
  }

  /**
   * Format the list of students as an HTML bullet list for the email.
   * Returns an HTML unordered list (&lt;ul&gt;) with each student as a list item (&lt;li&gt;).
   * If a user cannot be found or there's an error, that student will be
   * skipped with a log message.
   * @param entrantIds List of student user IDs
   * @return HTML formatted bullet list of students
   */
  private String formatStudentsList(final List<Long> entrantIds) {
    if (entrantIds == null || entrantIds.isEmpty()) {
      return "No students listed";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("<ul>");

    for (Long userId : entrantIds) {
      try {
        RegisteredUserDTO student = userAccountManager.getUserDTOById(userId);

        String firstName = student.getGivenName() != null ? student.getGivenName() : "";
        String lastName = student.getFamilyName() != null ? student.getFamilyName() : "";
        String fullName = (firstName + " " + lastName).trim();

        if (fullName.isEmpty()) {
          fullName = "Unknown name";
        }

        sb.append("<li>")
            .append(fullName)
            .append("</li>");

      } catch (NoUserException e) {
        log.warn("Could not find student with ID {} for competition entry confirmation email", userId);
        sb.append("<li>")
            .append("Student ID ")
            .append(userId)
            .append(" (user not found)")
            .append("</li>");
      } catch (Exception e) {
        log.error("Error looking up student ID {} for email", userId, e);
      }
    }

    sb.append("</ul>");

    // If only the opening and closing tags exist, return fallback message
    String result = sb.toString();
    if (result.equals("<ul></ul>")) {
      return "No students listed";
    }

    return result;
  }
}