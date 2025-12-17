/**
 * Copyright 2021 James Sharkey
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

package uk.ac.cam.cl.dtg.segue.api.managers;

import com.mailjet.client.errors.MailjetClientCommunicationException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetRateLimitException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IExternalAccountDataManager;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;
import uk.ac.cam.cl.dtg.util.email.MailJetSubscriptionAction;

public class ExternalAccountManager implements IExternalAccountManager {
  private static final Logger log = LoggerFactory.getLogger(ExternalAccountManager.class);

  private final IExternalAccountDataManager database;
  private final MailJetApiClientWrapper mailjetApi;

  /**
   * Synchronise account settings, email preferences and verification status with third party providers.
   * <br>
   * Currently this class is highly specialised for synchronising with MailJet.
   *
   * @param mailjetApi - to enable updates on MailJet
   * @param database   - to persist external identifiers and to record sync success.
   */
  public ExternalAccountManager(final MailJetApiClientWrapper mailjetApi, final IExternalAccountDataManager database) {
    this.database = database;
    this.mailjetApi = mailjetApi;
  }

  /**
   * Synchronise account settings and data with external providers.
   * <br>
   * Whilst the actions this method takes are mostly idempotent, it should not be run simultaneously with itself.
   *
   * @throws ExternalAccountSynchronisationException on unrecoverable errors with external providers.
   */
  @Override
  public synchronized void synchroniseChangedUsers() throws ExternalAccountSynchronisationException {
    log.info("MMAILJETT - === Starting Mailjet user synchronisation ===");
    int totalUsersProcessed = 0;
    int successfulSyncs = 0;
    int failedSyncs = 0;
    int skippedUsers = 0;

    try {
      List<UserExternalAccountChanges> userRecordsToUpdate = database.getRecentlyChangedRecords();
      log.info("MMAILJETT - Found {} users requiring synchronisation", userRecordsToUpdate.size());

      for (UserExternalAccountChanges userRecord : userRecordsToUpdate) {
        totalUsersProcessed++;
        Long userId = userRecord.getUserId();
        String accountEmail = userRecord.getAccountEmail();

        log.info("MMAILJETT - [User {}] Starting sync for email: {}", userId, sanitiseEmailForLogging(accountEmail));
        log.info("MMAILJETT - [User {}] Full user record: deleted={}, emailVerificationStatus={}, providerUserId={}, " +
                "allowsNews={}, allowsEvents={}, givenName={}, role={}, stage={}",
            userId,
            userRecord.isDeleted(),
            userRecord.getEmailVerificationStatus(),
            userRecord.getProviderUserId(),
            userRecord.allowsNewsEmails(),
            userRecord.allowsEventsEmails(),
            userRecord.getGivenName(),
            userRecord.getRole(),
            userRecord.getStage());

        try {
          boolean syncSuccess = processSingleUser(userRecord);

          if (syncSuccess) {
            successfulSyncs++;
            log.info("MMAILJETT - [User {}] Successfully synced to Mailjet", userId);
          } else {
            skippedUsers++;
            log.info("MMAILJETT - [User {}] Skipped (invalid/deleted user not requiring Mailjet)", userId);
          }

        } catch (SegueDatabaseException e) {
          failedSyncs++;
          log.error(String.format("MMAILJETT - [User %s] Database error during sync - will retry on next run", userId), e);
          // Don't update provider_last_updated so it will be retried

        } catch (MailjetClientCommunicationException e) {
          failedSyncs++;
          log.error(String.format("MMAILJETT - [User %s] Failed to communicate with Mailjet API", userId), e);
          log.error("MMAILJETT - [User {}] MailjetClientCommunicationException details: {}", userId, e.getMessage());
          throw new ExternalAccountSynchronisationException("Failed to successfully connect to MailJet!");

        } catch (MailjetRateLimitException e) {
          failedSyncs++;
          log.warn(String.format("MMAILJETT - [User %s] Hit Mailjet rate limit - stopping sync", userId), e);
          throw new ExternalAccountSynchronisationException("MailJet API rate limits exceeded!");

        } catch (MailjetException e) {
          failedSyncs++;
          log.error(String.format("MMAILJETT - [User %s] Mailjet API error during sync", userId), e);
          log.error("MMAILJETT - [User {}] MailjetException type: {}, message: {}",
              userId, e.getClass().getName(), e.getMessage());
          // Log the full stack trace for debugging
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          log.error("MMAILJETT - [User {}] Full stack trace: {}", userId, sw.toString());

          // Don't throw - continue processing other users, but don't update provider_last_updated
          // so this user will be retried next time

        } catch (Exception e) {
          failedSyncs++;
          log.error(String.format("MMAILJETT - [User %s] Unexpected error during sync", userId), e);
          log.error("MMAILJETT - [User {}] Unexpected exception type: {}, message: {}",
              userId, e.getClass().getName(), e.getMessage());
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          log.error("MMAILJETT - [User {}] Full stack trace: {}", userId, sw.toString());
          // Continue processing other users
        }
      }

    } catch (SegueDatabaseException e) {
      log.error("MMAILJETT - Database error whilst collecting users whose details have changed!", e);
      throw new ExternalAccountSynchronisationException("MMAILJETT - Database error during user collection");
    }

    log.info("MMAILJETT - === Mailjet synchronisation complete ===");
    log.info("MMAILJETT - Total users processed: {}", totalUsersProcessed);
    log.info("MMAILJETT - Successful syncs: {}", successfulSyncs);
    log.info("MMAILJETT - Failed syncs: {}", failedSyncs);
    log.info("MMAILJETT - Skipped users: {}", skippedUsers);
  }

  /**
   * Process a single user synchronisation.
   *
   * @param userRecord the user record to process
   * @return true if sync succeeded, false if user was skipped
   * @throws SegueDatabaseException if database operations fail
   * @throws MailjetException if Mailjet API calls fail
   */
  private boolean processSingleUser(UserExternalAccountChanges userRecord)
      throws SegueDatabaseException, MailjetException {

    Long userId = userRecord.getUserId();
    String accountEmail = userRecord.getAccountEmail();
    boolean accountEmailDeliveryFailed =
        EmailVerificationStatus.DELIVERY_FAILED.equals(userRecord.getEmailVerificationStatus());
    String mailjetId = userRecord.getProviderUserId();

    // Validate user data before attempting sync
    if (!validateUserData(userRecord)) {
      log.warn("MMAILJETT - [User {}] Skipping sync due to invalid user data", userId);
      // Still update provider_last_updated to prevent repeated attempts
      database.updateProviderLastUpdated(userId);
      return false;
    }

    if (null != mailjetId) {
      log.info("MMAILJETT - [User {}] Existing Mailjet user with ID: {}", userId, mailjetId);

      // Verify the user still exists in Mailjet
      JSONObject mailjetDetails = mailjetApi.getAccountByIdOrEmail(mailjetId);

      if (mailjetDetails == null) {
        log.warn("MMAILJETT - [User {}] Mailjet ID {} not found in Mailjet - will recreate", userId, mailjetId);
        // Clear the mailjet ID and recreate
        database.updateExternalAccount(userId, null);
        mailjetId = null;
      } else {
        log.warn("MMAILJETT - [User {}] Found existing Mailjet account: {}", userId, mailjetDetails.toString());
      }
    }

    if (null != mailjetId) {
      // User exists in Mailjet - handle updates/deletions
      JSONObject mailjetDetails = mailjetApi.getAccountByIdOrEmail(mailjetId);

      if (userRecord.isDeleted()) {
        log.info("MMAILJETT - [User {}] Processing deletion from Mailjet", userId);
        deleteUserFromMailJet(mailjetId, userRecord);

      } else if (accountEmailDeliveryFailed) {
        log.info("MMAILJETT - [User {}] Email delivery failed - removing from lists", userId);
        mailjetApi.updateUserSubscriptions(mailjetId, MailJetSubscriptionAction.REMOVE,
            MailJetSubscriptionAction.REMOVE);
        database.updateProviderLastUpdated(userId);

      } else if (!accountEmail.toLowerCase().equals(mailjetDetails.getString("Email"))) {
        log.info("MMAILJETT - [User {}] Email changed from {} to {} - recreating Mailjet contact",
            userId, sanitiseEmailForLogging(mailjetDetails.getString("Email")),
            sanitiseEmailForLogging(accountEmail));
        mailjetApi.permanentlyDeleteAccountById(mailjetId);
        log.info("MMAILJETT - [User {}] Deleted old Mailjet contact", userId);

        mailjetId = mailjetApi.addNewUserOrGetUserIfExists(accountEmail);
        log.info("MMAILJETT - [User {}] Created new Mailjet contact with ID: {}", userId, mailjetId);

        updateUserOnMailJet(mailjetId, userRecord);

      } else {
        log.info("MMAILJETT - [User {}] Updating existing Mailjet contact", userId);
        updateUserOnMailJet(mailjetId, userRecord);
      }

    } else {
      // User doesn't exist in Mailjet yet
      if (!accountEmailDeliveryFailed && !userRecord.isDeleted()) {
        log.info("MMAILJETT - [User {}] New user - creating Mailjet contact", userId);

        // Create the contact
        mailjetId = mailjetApi.addNewUserOrGetUserIfExists(accountEmail);

        if (mailjetId == null) {
          log.error("MMAILJETT - [User {}] Failed to create Mailjet contact - addNewUserOrGetUserIfExists returned null", userId);
          throw new MailjetException("Failed to create Mailjet contact - returned null ID");
        }

        log.info("MMAILJETT - [User {}] Created Mailjet contact with ID: {}", userId, mailjetId);

        // Now update properties and subscriptions
        // Use try-finally to ensure we record the mailjet ID even if updates fail
        boolean updateSucceeded = false;
        try {
          updateUserOnMailJet(mailjetId, userRecord);
          updateSucceeded = true;
        } finally {
          if (!updateSucceeded) {
            log.error("MMAILJETT - [User {}] Failed to update properties/subscriptions for Mailjet ID {} - " +
                "contact exists but may have incomplete data", userId, mailjetId);
            // Store the mailjet ID anyway so we don't create duplicates
            database.updateExternalAccount(userId, mailjetId);
          }
        }

      } else {
        log.info("MMAILJETT - [User {}] Skipping - delivery failed or deleted (not uploading to Mailjet)", userId);
        database.updateExternalAccount(userId, null);
        database.updateProviderLastUpdated(userId);
        return false;
      }
    }

    // If we got here, sync succeeded
    database.updateProviderLastUpdated(userId);
    return true;
  }

  /**
   * Validate user data before attempting Mailjet sync.
   *
   * @param userRecord the user record to validate
   * @return true if data is valid, false otherwise
   */
  private boolean validateUserData(UserExternalAccountChanges userRecord) {
    Long userId = userRecord.getUserId();
    String accountEmail = userRecord.getAccountEmail();

    if (accountEmail == null || accountEmail.trim().isEmpty()) {
      log.error("MMAILJETT - [User {}] Invalid data: email is null or empty", userId);
      return false;
    }

    if (!accountEmail.contains("@")) {
      log.error("MMAILJETT - [User {}] Invalid data: email '{}' does not contain @",
          userId, sanitiseEmailForLogging(accountEmail));
      return false;
    }

    // Check for required fields (allowing nulls but logging them)
    if (userRecord.getGivenName() == null) {
      log.warn("MMAILJETT - [User {}] Warning: given_name is null", userId);
    }

    if (userRecord.getRole() == null) {
      log.warn("MMAILJETT - [User {}] Warning: role is null", userId);
    }

    if (userRecord.getStage() == null) {
      log.warn("MMAILJETT - [User {}] Warning: stage is null", userId);
    }

    if (userRecord.getEmailVerificationStatus() == null) {
      log.warn("MMAILJETT - [User {}] Warning: email_verification_status is null", userId);
    }

    return true;
  }

  private void updateUserOnMailJet(final String mailjetId, final UserExternalAccountChanges userRecord)
      throws SegueDatabaseException, MailjetException {

    Long userId = userRecord.getUserId();
    log.info("MMAILJETT - [User {}] Updating properties for Mailjet ID: {}", userId, mailjetId);

    // Update properties
    try {
      String firstName = userRecord.getGivenName();
      String role = userRecord.getRole() != null ? userRecord.getRole().toString() : "";
      String verificationStatus = userRecord.getEmailVerificationStatus() != null
          ? userRecord.getEmailVerificationStatus().toString() : "";
      String stage = userRecord.getStage();

      log.info("MMAILJETT - [User {}] Setting properties - firstName: {}, role: {}, verificationStatus: {}, stage: {}",
          userId, firstName, role, verificationStatus, stage);

      mailjetApi.updateUserProperties(mailjetId, firstName, role, verificationStatus, stage);
      log.info("MMAILJETT - [User {}] Successfully updated Mailjet properties", userId);

    } catch (MailjetException e) {
      log.error("MMAILJETT - [User {}] Failed to update Mailjet properties for ID {}", userId, mailjetId, e);
      throw e;
    }

    // Update subscriptions
    try {
      MailJetSubscriptionAction newsStatus = (userRecord.allowsNewsEmails() != null
          && userRecord.allowsNewsEmails()) ? MailJetSubscriptionAction.FORCE_SUBSCRIBE :
          MailJetSubscriptionAction.UNSUBSCRIBE;
      MailJetSubscriptionAction eventsStatus = (userRecord.allowsEventsEmails() != null
          && userRecord.allowsEventsEmails()) ? MailJetSubscriptionAction.FORCE_SUBSCRIBE :
          MailJetSubscriptionAction.UNSUBSCRIBE;

      log.info("MMAILJETT - [User {}] Setting subscriptions - news: {}, events: {}",
          userId, newsStatus, eventsStatus);

      mailjetApi.updateUserSubscriptions(mailjetId, newsStatus, eventsStatus);
      log.info("MMAILJETT - [User {}] Successfully updated Mailjet subscriptions", userId);

    } catch (MailjetException e) {
      log.error("MMAILJETT - [User {}] Failed to update Mailjet subscriptions for ID {}", userId, mailjetId, e);
      throw e;
    }

    // Store the Mailjet ID
    try {
      database.updateExternalAccount(userId, mailjetId);
      log.info("MMAILJETT - [User {}] Stored Mailjet ID in database: {}", userId, mailjetId);
    } catch (SegueDatabaseException e) {
      log.error("MMAILJETT - [User {}] Failed to store Mailjet ID {} in database", userId, mailjetId, e);
      throw e;
    }
  }

  private void deleteUserFromMailJet(final String mailjetId, final UserExternalAccountChanges userRecord)
      throws SegueDatabaseException, MailjetException {
    Long userId = userRecord.getUserId();
    log.info("MMAILJETT - [User {}] Deleting from Mailjet, ID: {}", userId, mailjetId);

    try {
      mailjetApi.permanentlyDeleteAccountById(mailjetId);
      log.info("MMAILJETT - [User {}] Successfully deleted from Mailjet", userId);
    } catch (MailjetException e) {
      log.error("MMAILJETT - [User {}] Failed to delete from Mailjet, ID: {}", userId, mailjetId, e);
      throw e;
    }

    try {
      database.updateExternalAccount(userId, null);
      log.info("MMAILJETT - [User {}] Cleared Mailjet ID from database", userId);
    } catch (SegueDatabaseException e) {
      log.error("MMAILJETT - [User {}] Failed to clear Mailjet ID from database", userId, e);
      throw e;
    }
  }

  /**
   * Sanitise email for logging to prevent log injection and reduce PII exposure.
   *
   * @param email the email to sanitise
   * @return sanitised email (e.g., "u***@example.com")
   */
  private String sanitiseEmailForLogging(String email) {
    if (email == null) {
      return "null";
    }
    if (!email.contains("@")) {
      return "invalid-email";
    }
    String[] parts = email.split("@");
    if (parts[0].length() <= 2) {
      return parts[0].charAt(0) + "***@" + parts[1];
    }
    return parts[0].charAt(0) + "***@" + parts[1];
  }
}