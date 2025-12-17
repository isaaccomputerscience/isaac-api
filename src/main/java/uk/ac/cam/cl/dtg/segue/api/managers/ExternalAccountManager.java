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

import org.json.JSONException;
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
    long startTime = System.currentTimeMillis();
    log.info("MAILJET - === Starting Mailjet user synchronisation ===");
    int totalUsersProcessed = 0;
    int successfulSyncs = 0;
    int failedSyncs = 0;
    int skippedUsers = 0;

    try {
      List<UserExternalAccountChanges> userRecordsToUpdate = database.getRecentlyChangedRecords();
      log.info("MAILJET - Found {} users requiring synchronisation", userRecordsToUpdate.size());

      for (UserExternalAccountChanges userRecord : userRecordsToUpdate) {
        totalUsersProcessed++;
        Long userId = userRecord.getUserId();
        String accountEmail = userRecord.getAccountEmail();

        // Rate limiting: 2 second delay between each user
        if (totalUsersProcessed > 1) {
          try {
            Thread.sleep(2000); // 2 seconds between users
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("MAILJET - Thread interrupted during rate limit delay");
          }
        }

        // Additional delay every 100 users to avoid rate limits
        if (totalUsersProcessed % 100 == 0) {
          try {
            log.info("MAILJET - Processed {} users, pausing 10 seconds to avoid rate limits", totalUsersProcessed);
            Thread.sleep(10000); // 10 seconds every 100 users
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("MAILJET - Thread interrupted during extended rate limit delay");
          }
        }

        log.debug("MAILJET - [User {}] Starting sync for email: {}", userId, sanitiseEmailForLogging(accountEmail));
        log.debug("MAILJET - [User {}] Details: deleted={}, emailStatus={}, mailjetId={}, allowsNews={}, allowsEvents={}, role={}, stage={}",
                userId,
                userRecord.isDeleted(),
                userRecord.getEmailVerificationStatus(),
                userRecord.getProviderUserId(),
                userRecord.allowsNewsEmails(),
                userRecord.allowsEventsEmails(),
                userRecord.getRole(),
                userRecord.getStage());

        try {
          boolean syncSuccess = processSingleUser(userRecord);

          if (syncSuccess) {
            successfulSyncs++;
            log.info("MAILJET - [User {}] Successfully synced to Mailjet", userId);
          } else {
            skippedUsers++;
            log.debug("MAILJET - [User {}] Skipped (invalid/deleted/unsubscribed user)", userId);
          }

        } catch (SegueDatabaseException e) {
          failedSyncs++;
          log.error("MAILJET - [User {}] Database error during sync - will retry on next run: {}", userId, e.getMessage());
          // Don't update provider_last_updated so it will be retried

        } catch (MailjetClientCommunicationException e) {
          failedSyncs++;
          log.error("MAILJET - [User {}] Failed to communicate with Mailjet API: {}", userId, e.getMessage());
          throw new ExternalAccountSynchronisationException("Failed to successfully connect to MailJet!");

        } catch (MailjetRateLimitException e) {
          failedSyncs++;
          log.warn("MAILJET - [User {}] Hit Mailjet rate limit - stopping sync", userId);
          throw new ExternalAccountSynchronisationException("MailJet API rate limits exceeded!");

        } catch (MailjetException e) {
          failedSyncs++;
          log.error("MAILJET - [User {}] Mailjet API error: {} - {}", userId, e.getClass().getSimpleName(), e.getMessage());
          // Log stack trace at debug level to avoid flooding logs
          log.debug("MAILJET - [User {}] Full stack trace:", userId, e);
          // Don't throw - continue processing other users

        } catch (Exception e) {
          failedSyncs++;
          log.error("MAILJET - [User {}] Unexpected error: {} - {}", userId, e.getClass().getSimpleName(), e.getMessage());
          log.debug("MAILJET - [User {}] Full stack trace:", userId, e);
          // Continue processing other users
        }
      }

    } catch (SegueDatabaseException e) {
      log.error("MAILJET - Database error whilst collecting users: {}", e.getMessage(), e);
      throw new ExternalAccountSynchronisationException("Database error during user collection");
    }

    log.info("MAILJET - === Mailjet synchronisation complete ===");
    log.info("MAILJET - Total users processed: {}", totalUsersProcessed);
    log.info("MAILJET - Successful syncs: {}", successfulSyncs);
    log.info("MAILJET - Failed syncs: {}", failedSyncs);
    log.info("MAILJET - Skipped users: {}", skippedUsers);

    long duration = (System.currentTimeMillis() - startTime) / 1000;
    log.info("MAILJET - Sync duration: {} seconds ({} minutes)", duration, duration / 60);
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
      log.warn("MAILJET - [User {}] Skipping sync due to invalid user data", userId);
      // Still update provider_last_updated to prevent repeated attempts
      database.updateProviderLastUpdated(userId);
      return false;
    }

    // Check if user should be synced to Mailjet at all
    // Skip users who are not subscribed to any emails and don't already exist in Mailjet
    if (mailjetId == null && !shouldSyncToMailjet(userRecord)) {
      log.debug("MAILJET - [User {}] Skipping - user not subscribed to any emails", userId);
      database.updateProviderLastUpdated(userId);
      return false;
    }

    if (null != mailjetId) {
      log.debug("MAILJET - [User {}] Existing Mailjet user with ID: {}", userId, mailjetId);

      // Verify the user still exists in Mailjet
      try {
        JSONObject mailjetDetails = mailjetApi.getAccountByIdOrEmail(mailjetId);

        if (mailjetDetails == null) {
          log.warn("MAILJET - [User {}] Mailjet ID {} not found - will recreate if needed", userId, mailjetId);
          database.updateExternalAccount(userId, null);
          mailjetId = null;
        } else {
          log.debug("MAILJET - [User {}] Found existing Mailjet account", userId);
        }
      } catch (MailjetException e) {
        // If we get 404, clear the ID
        if (is404Error(e)) {
          log.warn("MAILJET - [User {}] Mailjet contact {} returned 404 - clearing stale ID", userId, mailjetId);
          database.updateExternalAccount(userId, null);
          mailjetId = null;
        } else {
          throw e;
        }
      }
    }

    if (null != mailjetId) {
      // User exists in Mailjet - handle updates/deletions
      return handleExistingMailjetUser(userRecord, mailjetId);

    } else {
      // User doesn't exist in Mailjet yet
      return handleNewMailjetUser(userRecord);
    }
  }

  /**
   * Determines if a user should be synced to Mailjet.
   * Users should be synced if they are subscribed to any emails OR if they already exist in Mailjet.
   */
  private boolean shouldSyncToMailjet(UserExternalAccountChanges userRecord) {
    // Don't sync deleted users or users with delivery failures
    if (userRecord.isDeleted() ||
            EmailVerificationStatus.DELIVERY_FAILED.equals(userRecord.getEmailVerificationStatus())) {
      return false;
    }

    // Sync if user is subscribed to news or events
    return (userRecord.allowsNewsEmails() != null && userRecord.allowsNewsEmails())
            || (userRecord.allowsEventsEmails() != null && userRecord.allowsEventsEmails());
  }

  /**
   * Handles sync for a user who already exists in Mailjet.
   */
  private boolean handleExistingMailjetUser(UserExternalAccountChanges userRecord, String mailjetId)
          throws SegueDatabaseException, MailjetException {

    Long userId = userRecord.getUserId();
    String accountEmail = userRecord.getAccountEmail();

    try {
      JSONObject mailjetDetails = mailjetApi.getAccountByIdOrEmail(mailjetId);

      if (userRecord.isDeleted()) {
        log.info("MAILJET - [User {}] Processing deletion from Mailjet", userId);
        deleteUserFromMailJet(mailjetId, userRecord);

      } else if (EmailVerificationStatus.DELIVERY_FAILED.equals(userRecord.getEmailVerificationStatus())) {
        log.info("MAILJET - [User {}] Email delivery failed - removing from lists", userId);
        mailjetApi.updateUserSubscriptions(mailjetId, MailJetSubscriptionAction.REMOVE,
                MailJetSubscriptionAction.REMOVE);
        database.updateProviderLastUpdated(userId);

      } else if (!accountEmail.toLowerCase().equals(mailjetDetails.getString("Email").toLowerCase())) {
        log.info("MAILJET - [User {}] Email changed - recreating Mailjet contact", userId);
        mailjetApi.permanentlyDeleteAccountById(mailjetId);
        log.debug("MAILJET - [User {}] Deleted old Mailjet contact", userId);

        mailjetId = mailjetApi.addNewUserOrGetUserIfExists(accountEmail);
        log.info("MAILJET - [User {}] Created new Mailjet contact with ID: {}", userId, mailjetId);

        updateUserOnMailJet(mailjetId, userRecord);

      } else {
        log.debug("MAILJET - [User {}] Updating existing Mailjet contact", userId);
        updateUserOnMailJet(mailjetId, userRecord);
      }

      database.updateProviderLastUpdated(userId);
      return true;

    } catch (JSONException e) {
      log.error("MAILJET - [User {}] Failed to parse Mailjet account details: {}", userId, e.getMessage());
      throw new MailjetException("Failed to parse Mailjet response", e);
    }
  }

  /**
   * Handles sync for a user who doesn't exist in Mailjet yet.
   */
  private boolean handleNewMailjetUser(UserExternalAccountChanges userRecord)
          throws SegueDatabaseException, MailjetException {

    Long userId = userRecord.getUserId();
    String accountEmail = userRecord.getAccountEmail();

    // Only create new contacts if user is subscribed
    if (!shouldSyncToMailjet(userRecord)) {
      log.debug("MAILJET - [User {}] Skipping creation - not subscribed to any emails", userId);
      database.updateProviderLastUpdated(userId);
      return false;
    }

    log.info("MAILJET - [User {}] New user - creating Mailjet contact", userId);

    // Create the contact
    String mailjetId = mailjetApi.addNewUserOrGetUserIfExists(accountEmail);

    if (mailjetId == null) {
      log.error("MAILJET - [User {}] Failed to create Mailjet contact - returned null ID", userId);
      throw new MailjetException("Failed to create Mailjet contact - returned null ID");
    }

    log.info("MAILJET - [User {}] Created Mailjet contact with ID: {}", userId, mailjetId);

    // Update properties and subscriptions
    boolean updateSucceeded = false;
    try {
      updateUserOnMailJet(mailjetId, userRecord);
      updateSucceeded = true;
    } finally {
      if (!updateSucceeded) {
        log.error("MAILJET - [User {}] Failed to update properties/subscriptions for Mailjet ID {} - contact exists but may have incomplete data",
                userId, mailjetId);
        // Store the mailjet ID anyway so we don't create duplicates
        database.updateExternalAccount(userId, mailjetId);
      }
    }

    database.updateProviderLastUpdated(userId);
    return true;
  }

  /**
   * Validate user data before attempting Mailjet sync.
   */
  private boolean validateUserData(UserExternalAccountChanges userRecord) {
    Long userId = userRecord.getUserId();
    String accountEmail = userRecord.getAccountEmail();

    if (accountEmail == null || accountEmail.trim().isEmpty()) {
      log.error("MAILJET - [User {}] Invalid data: email is null or empty", userId);
      return false;
    }

    if (!accountEmail.contains("@")) {
      log.error("MAILJET - [User {}] Invalid data: email '{}' does not contain @",
              userId, sanitiseEmailForLogging(accountEmail));
      return false;
    }

    // Log warnings for null fields but don't fail validation
    if (userRecord.getGivenName() == null) {
      log.debug("MAILJET - [User {}] Warning: given_name is null", userId);
    }

    if (userRecord.getRole() == null) {
      log.warn("MAILJET - [User {}] Warning: role is null", userId);
    }

    if (userRecord.getStage() == null) {
      log.debug("MAILJET - [User {}] Warning: stage is null", userId);
    }

    if (userRecord.getEmailVerificationStatus() == null) {
      log.warn("MAILJET - [User {}] Warning: email_verification_status is null", userId);
    }

    return true;
  }

  private void updateUserOnMailJet(final String mailjetId, final UserExternalAccountChanges userRecord)
          throws SegueDatabaseException, MailjetException {

    Long userId = userRecord.getUserId();
    log.debug("MAILJET - [User {}] Updating properties for Mailjet ID: {}", userId, mailjetId);

    // Update properties
    try {
      String firstName = userRecord.getGivenName();
      String role = userRecord.getRole() != null ? userRecord.getRole().toString() : "";
      String verificationStatus = userRecord.getEmailVerificationStatus() != null
              ? userRecord.getEmailVerificationStatus().toString() : "";
      String stage = userRecord.getStage() != null ? userRecord.getStage() : "not_specified";

      log.debug("MAILJET - [User {}] Setting properties - firstName: {}, role: {}, stage: {}",
              userId, firstName, role, stage);

      mailjetApi.updateUserProperties(mailjetId, firstName, role, verificationStatus, stage);
      log.debug("MAILJET - [User {}] Successfully updated Mailjet properties", userId);

    } catch (MailjetException e) {
      if (is404Error(e)) {
        log.warn("MAILJET - [User {}] Contact {} not found (404) during property update - clearing stale ID",
                userId, mailjetId);
        handleStaleMailjetId(userId, mailjetId);
        return;
      }
      log.error("MAILJET - [User {}] Failed to update Mailjet properties for ID {}", userId, mailjetId);
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

      log.debug("MAILJET - [User {}] Setting subscriptions - news: {}, events: {}",
              userId, newsStatus, eventsStatus);

      mailjetApi.updateUserSubscriptions(mailjetId, newsStatus, eventsStatus);
      log.debug("MAILJET - [User {}] Successfully updated Mailjet subscriptions", userId);

    } catch (MailjetException e) {
      if (is404Error(e)) {
        log.warn("MAILJET - [User {}] Contact {} not found (404) during subscription update - clearing stale ID",
                userId, mailjetId);
        handleStaleMailjetId(userId, mailjetId);
        return;
      }
      log.error("MAILJET - [User {}] Failed to update Mailjet subscriptions for ID {}", userId, mailjetId);
      throw e;
    }

    // Store the Mailjet ID
    try {
      database.updateExternalAccount(userId, mailjetId);
      log.debug("MAILJET - [User {}] Stored Mailjet ID in database: {}", userId, mailjetId);
    } catch (SegueDatabaseException e) {
      log.error("MAILJET - [User {}] Failed to store Mailjet ID {} in database", userId, mailjetId);
      throw e;
    }
  }

  /**
   * Checks if a MailjetException is a 404 error.
   */
  private boolean is404Error(MailjetException e) {
    if (e == null || e.getMessage() == null) {
      return false;
    }

    String message = e.getMessage();
    return message.contains("\"StatusCode\" : 404") ||
            message.contains("\"StatusCode\": 404") ||
            message.contains("Object not found") ||
            message.contains("404");
  }

  /**
   * Handles a stale Mailjet ID by clearing it from the database.
   */
  private void handleStaleMailjetId(Long userId, String staleMailjetId) throws SegueDatabaseException {
    log.info("MAILJET - [User {}] Clearing stale Mailjet ID: {}", userId, staleMailjetId);

    try {
      database.updateExternalAccount(userId, null);
      log.info("MAILJET - [User {}] Cleared stale Mailjet ID. Will be recreated on next sync.", userId);
    } catch (SegueDatabaseException e) {
      log.error("MAILJET - [User {}] CRITICAL: Failed to clear stale Mailjet ID {}", userId, staleMailjetId);
      throw e;
    }
  }

  private void deleteUserFromMailJet(final String mailjetId, final UserExternalAccountChanges userRecord)
          throws SegueDatabaseException, MailjetException {
    Long userId = userRecord.getUserId();
    log.info("MAILJET - [User {}] Deleting from Mailjet, ID: {}", userId, mailjetId);

    try {
      mailjetApi.permanentlyDeleteAccountById(mailjetId);
      log.info("MAILJET - [User {}] Successfully deleted from Mailjet", userId);
    } catch (MailjetException e) {
      // If already deleted (404), that's fine
      if (is404Error(e)) {
        log.info("MAILJET - [User {}] Contact already deleted from Mailjet (404)", userId);
      } else {
        log.error("MAILJET - [User {}] Failed to delete from Mailjet, ID: {}", userId, mailjetId);
        throw e;
      }
    }

    try {
      database.updateExternalAccount(userId, null);
      log.debug("MAILJET - [User {}] Cleared Mailjet ID from database", userId);
    } catch (SegueDatabaseException e) {
      log.error("MAILJET - [User {}] Failed to clear Mailjet ID from database", userId);
      throw e;
    }
  }

  /**
   * Sanitise email for logging to prevent log injection and reduce PII exposure.
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