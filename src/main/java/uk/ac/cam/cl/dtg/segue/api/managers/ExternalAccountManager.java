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
    log.info("Starting Mailjet synchronization process");

    List<UserExternalAccountChanges> userRecordsToUpdate;
    try {
      userRecordsToUpdate = database.getRecentlyChangedRecords();
      log.info("Found {} users to synchronize with Mailjet", userRecordsToUpdate.size());
    } catch (SegueDatabaseException e) {
      log.error("Database error whilst collecting users whose details have changed!", e);
      throw new ExternalAccountSynchronisationException("Failed to retrieve users for synchronization: " + e);
    }

    if (userRecordsToUpdate.isEmpty()) {
      log.info("No users to synchronize. Exiting.");
      return;
    }

    SyncMetrics metrics = new SyncMetrics();

    for (UserExternalAccountChanges userRecord : userRecordsToUpdate) {
      Long userId = userRecord.getUserId();

      try {
        log.debug("Processing user ID: {} with email: {}", userId, maskEmail(userRecord.getAccountEmail()));
        processUserSync(userRecord, metrics);
        metrics.incrementSuccess();
        log.debug("Successfully processed user ID: {}", userId);

      } catch (SegueDatabaseException e) {
        metrics.incrementDatabaseError();
        log.error("Database error storing Mailjet update for user ID: {}. Error: {}",
                userId, e.getMessage(), e);
        // Continue processing other users - database errors shouldn't stop the entire sync

      } catch (MailjetClientCommunicationException e) {
        metrics.incrementCommunicationError();
        log.error("Failed to communicate with Mailjet while processing user ID: {}. Error: {}",
                userId, e.getMessage(), e);
        throw new ExternalAccountSynchronisationException(
                "Failed to successfully connect to Mailjet" + e);

      } catch (MailjetRateLimitException e) {
        metrics.incrementRateLimitError();
        log.warn("Mailjet rate limit exceeded while processing user ID: {}. Processed {} users before limit.",
                userId, metrics.getSuccessCount());
        throw new ExternalAccountSynchronisationException(
                "Mailjet API rate limits exceeded after processing " + metrics.getSuccessCount() + " users" + e);

      } catch (MailjetException e) {
        metrics.incrementMailjetError();
        log.error("Mailjet API error while processing user ID: {}. Error: {}",
                userId, e.getMessage(), e);
        throw new ExternalAccountSynchronisationException(
                "Mailjet API error: " + e.getMessage() + e);

      } catch (Exception e) {
        metrics.incrementUnexpectedError();
        log.error("Unexpected error processing user ID: {}. Error: {}",
                userId, e.getMessage(), e);
        // Don't throw - log and continue to avoid blocking all syncs
      }
    }

    logSyncSummary(metrics, userRecordsToUpdate.size());
  }

  /**
   * Process synchronization for a single user.
   */
  private void processUserSync(UserExternalAccountChanges userRecord, SyncMetrics metrics)
          throws SegueDatabaseException, MailjetException {

    Long userId = userRecord.getUserId();
    String accountEmail = userRecord.getAccountEmail();

    // Validate required fields
    if (accountEmail == null || accountEmail.trim().isEmpty()) {
      log.warn("User ID {} has null or empty email address. Skipping.", userId);
      metrics.incrementSkipped();
      return;
    }

    boolean accountEmailDeliveryFailed =
            EmailVerificationStatus.DELIVERY_FAILED.equals(userRecord.getEmailVerificationStatus());
    String mailjetId = userRecord.getProviderUserId();

    if (mailjetId != null && !mailjetId.trim().isEmpty()) {
      handleExistingMailjetUser(mailjetId, userRecord, accountEmail, accountEmailDeliveryFailed, metrics);
    } else {
      handleNewMailjetUser(userRecord, accountEmail, accountEmailDeliveryFailed, metrics);
    }

    // Update the provider_last_updated timestamp on success
    database.updateProviderLastUpdated(userId);
    log.debug("Updated provider_last_updated timestamp for user ID: {}", userId);
  }

  /**
   * Handle synchronization for users that already exist in Mailjet.
   */
  private void handleExistingMailjetUser(String mailjetId, UserExternalAccountChanges userRecord,
                                         String accountEmail, boolean accountEmailDeliveryFailed, SyncMetrics metrics)
          throws SegueDatabaseException, MailjetException {

    Long userId = userRecord.getUserId();

    // Fetch current Mailjet details
    JSONObject mailjetDetails = mailjetApi.getAccountByIdOrEmail(mailjetId);

    if (mailjetDetails == null) {
      log.warn("User ID {} has Mailjet ID {} but account not found in Mailjet. Treating as new user.",
              userId, mailjetId);
      // Mailjet account doesn't exist - clear the ID and treat as new
      database.updateExternalAccount(userId, null);
      handleNewMailjetUser(userRecord, accountEmail, accountEmailDeliveryFailed, metrics);
      return;
    }

    if (userRecord.isDeleted()) {
      log.info("User ID {} is deleted. Removing from Mailjet.", userId);
      deleteUserFromMailJet(mailjetId, userRecord);
      metrics.incrementDeleted();

    } else if (accountEmailDeliveryFailed) {
      log.info("User ID {} has delivery failed status. Unsubscribing from all lists.", userId);
      mailjetApi.updateUserSubscriptions(mailjetId,
              MailJetSubscriptionAction.REMOVE,
              MailJetSubscriptionAction.REMOVE);
      metrics.incrementUnsubscribed();

    } else if (!accountEmail.equalsIgnoreCase(mailjetDetails.getString("Email"))) {
      log.info("User ID {} changed email from {} to {}. Recreating Mailjet account.",
              userId, maskEmail(mailjetDetails.getString("Email")), maskEmail(accountEmail));
      mailjetApi.permanentlyDeleteAccountById(mailjetId);
      String newMailjetId = mailjetApi.addNewUserOrGetUserIfExists(accountEmail);

      if (newMailjetId == null) {
        throw new MailjetException("Failed to create new Mailjet account after email change for user: " + userId);
      }

      updateUserOnMailJet(newMailjetId, userRecord);
      metrics.incrementEmailChanged();

    } else {
      log.debug("User ID {} has updated details/preferences. Updating Mailjet.", userId);
      updateUserOnMailJet(mailjetId, userRecord);
      metrics.incrementUpdated();
    }
  }

  /**
   * Handle synchronization for users that don't exist in Mailjet yet.
   */
  private void handleNewMailjetUser(UserExternalAccountChanges userRecord,
                                    String accountEmail, boolean accountEmailDeliveryFailed, SyncMetrics metrics)
          throws SegueDatabaseException, MailjetException {

    Long userId = userRecord.getUserId();

    if (!accountEmailDeliveryFailed && !userRecord.isDeleted()) {
      log.info("Creating new Mailjet account for user ID {} with email {}",
              userId, maskEmail(accountEmail));

      String mailjetId = mailjetApi.addNewUserOrGetUserIfExists(accountEmail);

      if (mailjetId == null) {
        log.error("Failed to create Mailjet account for user ID {}. Mailjet returned null ID.", userId);
        throw new MailjetException("Mailjet returned null ID when creating account for user: " + userId);
      }

      updateUserOnMailJet(mailjetId, userRecord);
      metrics.incrementCreated();

    } else {
      log.debug("User ID {} not eligible for Mailjet (deleted={}, deliveryFailed={}). Skipping.",
              userId, userRecord.isDeleted(), accountEmailDeliveryFailed);
      database.updateExternalAccount(userId, null);
      metrics.incrementSkipped();
    }
  }

  /**
   * Update user details and subscriptions in Mailjet.
   */
  private void updateUserOnMailJet(final String mailjetId, final UserExternalAccountChanges userRecord)
          throws SegueDatabaseException, MailjetException {

    if (mailjetId == null || mailjetId.trim().isEmpty()) {
      throw new IllegalArgumentException("Mailjet ID cannot be null or empty");
    }

    Long userId = userRecord.getUserId();

    // Update user properties
    String stage = userRecord.getStage() != null ? userRecord.getStage() : "unknown";
    mailjetApi.updateUserProperties(
            mailjetId,
            userRecord.getGivenName(),
            userRecord.getRole().toString(),
            userRecord.getEmailVerificationStatus().toString(),
            stage
    );

    // Update subscriptions
    MailJetSubscriptionAction newsStatus = Boolean.TRUE.equals(userRecord.allowsNewsEmails())
            ? MailJetSubscriptionAction.FORCE_SUBSCRIBE
            : MailJetSubscriptionAction.UNSUBSCRIBE;

    MailJetSubscriptionAction eventsStatus = Boolean.TRUE.equals(userRecord.allowsEventsEmails())
            ? MailJetSubscriptionAction.FORCE_SUBSCRIBE
            : MailJetSubscriptionAction.UNSUBSCRIBE;

    mailjetApi.updateUserSubscriptions(mailjetId, newsStatus, eventsStatus);

    // Store the Mailjet ID in the database
    database.updateExternalAccount(userId, mailjetId);

    log.debug("Updated Mailjet account {} for user ID {} (news={}, events={})",
            mailjetId, userId, newsStatus, eventsStatus);
  }

  /**
   * Delete user from Mailjet (GDPR compliance).
   */
  private void deleteUserFromMailJet(final String mailjetId, final UserExternalAccountChanges userRecord)
          throws SegueDatabaseException, MailjetException {

    if (mailjetId == null || mailjetId.trim().isEmpty()) {
      log.warn("Attempted to delete user with null/empty Mailjet ID. User ID: {}", userRecord.getUserId());
      return;
    }

    Long userId = userRecord.getUserId();
    mailjetApi.permanentlyDeleteAccountById(mailjetId);
    database.updateExternalAccount(userId, null);

    log.info("Deleted Mailjet account {} for user ID {} (GDPR deletion)", mailjetId, userId);
  }

  /**
   * Mask email address for logging (show only first 3 chars and domain).
   */
  private String maskEmail(String email) {
    if (email == null || email.isEmpty()) {
      return "[empty]";
    }

    int atIndex = email.indexOf('@');
    if (atIndex <= 0) {
      return email.substring(0, Math.min(3, email.length())) + "***";
    }

    String localPart = email.substring(0, atIndex);
    String domain = email.substring(atIndex);
    String masked = localPart.substring(0, Math.min(3, localPart.length())) + "***";

    return masked + domain;
  }

  /**
   * Log summary of synchronization results.
   */
  private void logSyncSummary(SyncMetrics metrics, int totalUsers) {
    log.info("=== Mailjet Synchronization Complete ===");
    log.info("Total users to process: {}", totalUsers);
    log.info("Successfully processed: {}", metrics.getSuccessCount());
    log.info("  - Created: {}", metrics.getCreatedCount());
    log.info("  - Updated: {}", metrics.getUpdatedCount());
    log.info("  - Deleted: {}", metrics.getDeletedCount());
    log.info("  - Email changed: {}", metrics.getEmailChangedCount());
    log.info("  - Unsubscribed: {}", metrics.getUnsubscribedCount());
    log.info("  - Skipped: {}", metrics.getSkippedCount());
    log.info("Errors:");
    log.info("  - Database errors: {}", metrics.getDatabaseErrorCount());
    log.info("  - Communication errors: {}", metrics.getCommunicationErrorCount());
    log.info("  - Rate limit errors: {}", metrics.getRateLimitErrorCount());
    log.info("  - Mailjet API errors: {}", metrics.getMailjetErrorCount());
    log.info("  - Unexpected errors: {}", metrics.getUnexpectedErrorCount());
    log.info("========================================");
  }

  /**
   * Inner class to track synchronization metrics.
   */
  private static class SyncMetrics {
    private int successCount = 0;
    private int createdCount = 0;
    private int updatedCount = 0;
    private int deletedCount = 0;
    private int emailChangedCount = 0;
    private int unsubscribedCount = 0;
    private int skippedCount = 0;
    private int databaseErrorCount = 0;
    private int communicationErrorCount = 0;
    private int rateLimitErrorCount = 0;
    private int mailjetErrorCount = 0;
    private int unexpectedErrorCount = 0;

    void incrementSuccess() { successCount++; }
    void incrementCreated() { createdCount++; }
    void incrementUpdated() { updatedCount++; }
    void incrementDeleted() { deletedCount++; }
    void incrementEmailChanged() { emailChangedCount++; }
    void incrementUnsubscribed() { unsubscribedCount++; }
    void incrementSkipped() { skippedCount++; }
    void incrementDatabaseError() { databaseErrorCount++; }
    void incrementCommunicationError() { communicationErrorCount++; }
    void incrementRateLimitError() { rateLimitErrorCount++; }
    void incrementMailjetError() { mailjetErrorCount++; }
    void incrementUnexpectedError() { unexpectedErrorCount++; }

    int getSuccessCount() { return successCount; }
    int getCreatedCount() { return createdCount; }
    int getUpdatedCount() { return updatedCount; }
    int getDeletedCount() { return deletedCount; }
    int getEmailChangedCount() { return emailChangedCount; }
    int getUnsubscribedCount() { return unsubscribedCount; }
    int getSkippedCount() { return skippedCount; }
    int getDatabaseErrorCount() { return databaseErrorCount; }
    int getCommunicationErrorCount() { return communicationErrorCount; }
    int getRateLimitErrorCount() { return rateLimitErrorCount; }
    int getMailjetErrorCount() { return mailjetErrorCount; }
    int getUnexpectedErrorCount() { return unexpectedErrorCount; }
  }
}