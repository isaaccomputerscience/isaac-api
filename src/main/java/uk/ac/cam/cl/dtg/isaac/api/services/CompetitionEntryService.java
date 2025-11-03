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
 *
 * This service is responsible for sending automated confirmation emails
 * to teachers immediately after they submit a competition entry.
 *
 * The email includes details of the submission such as project title,
 * project link, group name, and list of students.
 *
 * @author Isaac Computer Science Team
 */
public class CompetitionEntryService {
  private static final Logger log = LoggerFactory.getLogger(CompetitionEntryService.class);

  private static final String EMAIL_TEMPLATE_ID = "email_competition_entry_confirmation";
  private static final String CONTACT_US_EMAIL = "contact@isaaccomputerscience.org";

  private final EmailManager emailManager;
  private final UserAccountManager userAccountManager;

  /**
   * Constructor for dependency injection.
   *
   * @param emailManager       Manager for sending templated emails
   * @param userAccountManager Manager for user account operations
   */
  @Inject
  public CompetitionEntryService(
      final EmailManager emailManager,
      final UserAccountManager userAccountManager) {
    this.emailManager = emailManager;
    this.userAccountManager = userAccountManager;
  }

  /**
   * Send confirmation email to teacher after competition entry submission.
   * <p>
   * This method extracts the necessary information from the CompetitionEntryDTO
   * and the event, then sends a confirmation email to the reserving user (teacher).
   * <p>
   * IMPORTANT: If email sending fails, this method logs the error but does NOT
   * throw an exception, ensuring the submission is still considered successful.
   * This is by design - email delivery issues should not cause submission failures.
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

      // Build email context with submission details
      final Map<String, Object> emailContext = buildEmailContext(event, entryDTO, reservingUser);

      // Load email template
      final EmailTemplateDTO emailTemplate = emailManager.getEmailTemplateDTO(EMAIL_TEMPLATE_ID);

      // Send email immediately to the reserving user (teacher)
      emailManager.sendTemplatedEmailToUser(
          reservingUser,
          emailTemplate,
          emailContext,
          EmailType.SYSTEM
      );

      log.info("Successfully sent competition entry confirmation email to teacher: {} {}, email: {}",
          reservingUser.getGivenName(), reservingUser.getFamilyName(), reservingUser.getEmail());

    } catch (ContentManagerException e) {
      // Template not found or content issue - log error but don't fail submission
      log.error("Failed to send competition entry confirmation: Email template '{}' not found or invalid",
          EMAIL_TEMPLATE_ID, e);
    } catch (SegueDatabaseException e) {
      // Database issue - log error but don't fail submission
      log.error("Failed to send competition entry confirmation: Database error for user ID {}",
          reservingUser.getId(), e);
    } catch (Exception e) {
      // Any other unexpected error - log but don't fail submission
      log.error("Unexpected error sending competition entry confirmation email for user ID {}",
          reservingUser.getId(), e);
    }
  }

  /**
   * Build the email context map with all template variables.
   * <p>
   * This creates a map containing all the variables that will be substituted
   * into the email template, including event details and submission data.
   * <p>
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

    // Format students list from entrant IDs
    final String studentsList = formatStudentsList(entryDTO.getEntrantIds());

    // Extract submission details from DTO
    final String projectTitle = entryDTO.getProjectTitle() != null ? entryDTO.getProjectTitle() : "";
    final String projectLink = entryDTO.getSubmissionURL() != null ? entryDTO.getSubmissionURL() : "";
    final String groupName = entryDTO.getGroupName() != null ? entryDTO.getGroupName() : "";

    // Build context map with all template variables
    // Note: givenName and sig will be automatically added by EmailManager
    // from user properties and globalStringTokens respectively
    return Map.of(
        "event", event,
        "givenName", reservingUser.getGivenName(),
        "projectTitle", projectTitle,
        "projectLink", projectLink,
        "groupName", groupName,
        "studentsList", studentsList,
        "contactUsURL", String.format("<a href=\"mailto:%s\">contact us</a>", CONTACT_US_EMAIL)
    );
  }

  /**
   * Format the list of students as a readable string for the email.
   * <p>
   * This method looks up each student by their user ID and formats their
   * full name on a new line with a bullet point.
   * <p>
   * Example output:
   * - John Smith
   * - Jane Doe
   * - Bob Johnson
   * <p>
   * If a user cannot be found or there's an error, that student will be
   * skipped with a log message.
   *
   * @param entrantIds List of student user IDs
   * @return Formatted string of students
   */
  private String formatStudentsList(final List<Long> entrantIds) {
    if (entrantIds == null || entrantIds.isEmpty()) {
      return "No students listed";
    }

    final StringBuilder sb = new StringBuilder();

    for (Long userId : entrantIds) {
      try {
        // Look up each student by their user ID
        RegisteredUserDTO student = userAccountManager.getUserDTOById(userId);

        String firstName = student.getGivenName() != null ? student.getGivenName() : "";
        String lastName = student.getFamilyName() != null ? student.getFamilyName() : "";

        sb.append("- ")
            .append(firstName)
            .append(" ")
            .append(lastName)
            .append("\n");

      } catch (NoUserException e) {
        // Student not found - log but continue with others
        log.warn("Could not find student with ID {} for competition entry confirmation email", userId);
        sb.append("- Student ID ")
            .append(userId)
            .append(" (user not found)\n");
      } catch (Exception e) {
        // Unexpected error - log but continue
        log.error("Error looking up student ID {} for email", userId, e);
      }
    }

    String result = sb.toString().trim();
    return result.isEmpty() ? "No students listed" : result;
  }
}