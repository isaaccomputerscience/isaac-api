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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private static final String MAILJET = "MAILJET - ";
  private static final int BULK_BATCH_SIZE = 1000;
  private static final int RATE_LIMIT_RETRY_SLEEP_MS = 10000; // 10 seconds

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
    log.info("{}Starting Mailjet synchronization process", MAILJET);

    List<UserExternalAccountChanges> userRecordsToUpdate;
    try {
      userRecordsToUpdate = database.getRecentlyChangedRecords();
      log.info("{}Found {} users to synchronize with Mailjet", MAILJET, userRecordsToUpdate.size());
    } catch (SegueDatabaseException e) {
      throw new ExternalAccountSynchronisationException("Failed to retrieve users for synchronization"
              + e.getMessage());
    }

    if (userRecordsToUpdate.isEmpty()) {
      log.info("{}No users to synchronize", MAILJET);
      return;
    }

    SyncMetrics metrics = new SyncMetrics();
    List<Long> successfullyProcessedUserIds = new ArrayList<>();

    // Separate users into deletions and syncs
    List<UserExternalAccountChanges> usersToDelete = new ArrayList<>();
    List<UserExternalAccountChanges> usersToSync = new ArrayList<>();

    for (UserExternalAccountChanges userRecord : userRecordsToUpdate) {
      if (userRecord.isDeleted() && userRecord.getProviderUserId() != null) {
        usersToDelete.add(userRecord);
      } else {
        usersToSync.add(userRecord);
      }
    }

    // Process deletions individually with backoff
    for (UserExternalAccountChanges userRecord : usersToDelete) {
      try {
        deleteUserFromMailJetWithBackoff(userRecord.getProviderUserId(), userRecord);
        database.updateProviderLastUpdated(userRecord.getUserId());
        metrics.incrementDeleted();
        successfullyProcessedUserIds.add(userRecord.getUserId());
      } catch (SegueDatabaseException e) {
        metrics.incrementDatabaseError();
        log.error("{}Database error during deletion for user ID: {}", MAILJET, userRecord.getUserId(), e);
      } catch (MailjetClientCommunicationException e) {
        metrics.incrementCommunicationError();
        throw new ExternalAccountSynchronisationException("Failed to connect to Mailjet: " + e.getMessage());
      } catch (MailjetException e) {
        metrics.incrementMailjetError();
        log.error("{}Mailjet API error during deletion for user ID: {}. Continuing.",
            MAILJET, userRecord.getUserId(), e);
      }
    }

    // Process syncs via bulk API grouped by subscription state
    try {
      Map<SubscriptionGroup, List<UserExternalAccountChanges>> groupedUsers =
          groupUsersBySubscriptionState(usersToSync);

      for (Map.Entry<SubscriptionGroup, List<UserExternalAccountChanges>> entry : groupedUsers.entrySet()) {
        SubscriptionGroup group = entry.getKey();
        List<UserExternalAccountChanges> groupUsers = entry.getValue();

        for (int i = 0; i < groupUsers.size(); i += BULK_BATCH_SIZE) {
          int endIndex = Math.min(i + BULK_BATCH_SIZE, groupUsers.size());
          List<UserExternalAccountChanges> batch = groupUsers.subList(i, endIndex);

          try {
            mailjetApi.bulkSyncUsers(batch, group.newsAction, group.eventsAction);
            for (UserExternalAccountChanges user : batch) {
              metrics.incrementSuccess();
              successfullyProcessedUserIds.add(user.getUserId());
            }
          } catch (MailjetRateLimitException e) {
            metrics.incrementRateLimitError();
            log.warn("{}Mailjet rate limit exceeded during bulk sync. Processed {} users so far.",
                MAILJET, metrics.getSuccessCount());
            throw new ExternalAccountSynchronisationException(
                "Mailjet API rate limits exceeded after processing " + metrics.getSuccessCount() + " users");
          } catch (MailjetException e) {
            metrics.incrementMailjetError();
            log.error("{}Mailjet API error during bulk sync of {} users. Continuing with next batch.",
                MAILJET, batch.size(), e);
          }
        }
      }
    } catch (ExternalAccountSynchronisationException e) {
      throw e;
    } catch (Exception e) {
      metrics.incrementUnexpectedError();
      log.error("{}Unexpected error during bulk sync", MAILJET, e);
    }

    // Batch mark all successfully processed users as synced
    try {
      if (!successfullyProcessedUserIds.isEmpty()) {
        database.batchMarkAsSynced(successfullyProcessedUserIds);
      }
    } catch (SegueDatabaseException e) {
      metrics.incrementDatabaseError();
      log.error("{}Database error marking {} users as synced", MAILJET, successfullyProcessedUserIds.size(), e);
    }

    logSyncSummary(metrics, userRecordsToUpdate.size());
  }

  /**
   * Delete user from Mailjet (GDPR compliance).
   */
  private void deleteUserFromMailJet(final String mailjetId, final UserExternalAccountChanges userRecord)
      throws SegueDatabaseException, MailjetException {

    if (mailjetId == null || mailjetId.trim().isEmpty()) {
      log.warn("{}Attempted to delete user with null/empty Mailjet ID. User ID: {}", MAILJET, userRecord.getUserId());
      return;
    }

    Long userId = userRecord.getUserId();
    mailjetApi.permanentlyDeleteAccountById(mailjetId);
    database.updateExternalAccount(userId, null);

    log.info("{}Deleted Mailjet account {} for user ID {} (GDPR deletion)", MAILJET, mailjetId, userId);
  }

  /**
   * Delete user from Mailjet with exponential backoff on rate limit.
   */
  private void deleteUserFromMailJetWithBackoff(final String mailjetId,
                                                 final UserExternalAccountChanges userRecord)
      throws SegueDatabaseException, MailjetException {
    try {
      deleteUserFromMailJet(mailjetId, userRecord);
    } catch (MailjetRateLimitException e) {
      log.warn("{}Rate limit on deletion, retrying after backoff...", MAILJET);
      try {
        Thread.sleep(RATE_LIMIT_RETRY_SLEEP_MS);
        deleteUserFromMailJet(mailjetId, userRecord);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new MailjetException("Interrupted during deletion backoff", ie);
      }
    }
  }

  /**
   * Group users by subscription state combination.
   */
  private Map<SubscriptionGroup, List<UserExternalAccountChanges>>
      groupUsersBySubscriptionState(final List<UserExternalAccountChanges> users) {
    Map<SubscriptionGroup, List<UserExternalAccountChanges>> groups = new HashMap<>();

    for (UserExternalAccountChanges user : users) {
      if (user.getAccountEmail() == null || user.getAccountEmail().trim().isEmpty()) {
        log.warn("{}User ID {} has null/empty email. Skipping.", MAILJET, user.getUserId());
        continue;
      }

      boolean deliveryFailed =
          EmailVerificationStatus.DELIVERY_FAILED.equals(user.getEmailVerificationStatus());

      MailJetSubscriptionAction newsAction;
      MailJetSubscriptionAction eventsAction;

      if (deliveryFailed) {
        newsAction = MailJetSubscriptionAction.REMOVE;
        eventsAction = MailJetSubscriptionAction.REMOVE;
      } else {
        newsAction = Boolean.TRUE.equals(user.allowsNewsEmails())
            ? MailJetSubscriptionAction.FORCE_SUBSCRIBE
            : MailJetSubscriptionAction.UNSUBSCRIBE;
        eventsAction = Boolean.TRUE.equals(user.allowsEventsEmails())
            ? MailJetSubscriptionAction.FORCE_SUBSCRIBE
            : MailJetSubscriptionAction.UNSUBSCRIBE;
      }

      SubscriptionGroup group = new SubscriptionGroup(newsAction, eventsAction);
      groups.computeIfAbsent(group, k -> new ArrayList<>()).add(user);
    }

    return groups;
  }

  /**
   * Log summary of synchronization results.
   */
  private void logSyncSummary(SyncMetrics metrics, int totalUsers) {
    log.info("{}=== Mailjet Synchronization Complete ===", MAILJET);
    log.info("{}Total users to process: {}", MAILJET, totalUsers);
    log.info("{}Successfully processed: {}", MAILJET, metrics.getSuccessCount());
    log.info("{}  - Created: {}", MAILJET, metrics.getCreatedCount());
    log.info("{}  - Updated: {}", MAILJET, metrics.getUpdatedCount());
    log.info("{}  - Deleted: {}", MAILJET, metrics.getDeletedCount());
    log.info("{}  - Email changed: {}", MAILJET, metrics.getEmailChangedCount());
    log.info("{}  - Unsubscribed: {}", MAILJET, metrics.getUnsubscribedCount());
    log.info("{}  - Skipped: {}", MAILJET, metrics.getSkippedCount());
    log.info("{}Errors:", MAILJET);
    log.info("{}  - Database errors: {}", MAILJET, metrics.getDatabaseErrorCount());
    log.info("{}  - Communication errors: {}", MAILJET, metrics.getCommunicationErrorCount());
    log.info("{}  - Rate limit errors: {}", MAILJET, metrics.getRateLimitErrorCount());
    log.info("{}  - Mailjet API errors: {}", MAILJET, metrics.getMailjetErrorCount());
    log.info("{}  - Unexpected errors: {}", MAILJET, metrics.getUnexpectedErrorCount());
    log.info("{}========================================", MAILJET);
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

    void incrementSuccess() {
      successCount++;
    }

    void incrementCreated() {
      createdCount++;
    }

    void incrementUpdated() {
      updatedCount++;
    }

    void incrementDeleted() {
      deletedCount++;
    }

    void incrementEmailChanged() {
      emailChangedCount++;
    }

    void incrementUnsubscribed() {
      unsubscribedCount++;
    }

    void incrementSkipped() {
      skippedCount++;
    }

    void incrementDatabaseError() {
      databaseErrorCount++;
    }

    void incrementCommunicationError() {
      communicationErrorCount++;
    }

    void incrementRateLimitError() {
      rateLimitErrorCount++;
    }

    void incrementMailjetError() {
      mailjetErrorCount++;
    }

    void incrementUnexpectedError() {
      unexpectedErrorCount++;
    }

    int getSuccessCount() {
      return successCount;
    }

    int getCreatedCount() {
      return createdCount;
    }

    int getUpdatedCount() {
      return updatedCount;
    }

    int getDeletedCount() {
      return deletedCount;
    }

    int getEmailChangedCount() {
      return emailChangedCount;
    }

    int getUnsubscribedCount() {
      return unsubscribedCount;
    }

    int getSkippedCount() {
      return skippedCount;
    }

    int getDatabaseErrorCount() {
      return databaseErrorCount;
    }

    int getCommunicationErrorCount() {
      return communicationErrorCount;
    }

    int getRateLimitErrorCount() {
      return rateLimitErrorCount;
    }

    int getMailjetErrorCount() {
      return mailjetErrorCount;
    }

    int getUnexpectedErrorCount() {
      return unexpectedErrorCount;
    }
  }

  /**
   * Key for grouping users by their subscription preferences.
   */
  private static class SubscriptionGroup {
    final MailJetSubscriptionAction newsAction;
    final MailJetSubscriptionAction eventsAction;

    SubscriptionGroup(final MailJetSubscriptionAction newsAction,
                      final MailJetSubscriptionAction eventsAction) {
      this.newsAction = newsAction;
      this.eventsAction = eventsAction;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SubscriptionGroup that = (SubscriptionGroup) o;
      return newsAction == that.newsAction && eventsAction == that.eventsAction;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(newsAction, eventsAction);
    }
  }
}