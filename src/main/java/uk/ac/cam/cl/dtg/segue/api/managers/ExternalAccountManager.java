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
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IExternalAccountDataManager;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper.JobStatus;
import uk.ac.cam.cl.dtg.util.email.MailJetSubscriptionAction;

public class ExternalAccountManager implements IExternalAccountManager {
  private static final Logger log = LoggerFactory.getLogger(ExternalAccountManager.class);
  private static final String MAILJET = "MAILJET - ";
  private static final int BULK_BATCH_SIZE = 100;
  private static final int RATE_LIMIT_RETRY_SLEEP_MS = 10000; // 10 seconds
  private static final int JOB_POLL_INTERVAL_MS = 5000;  // 5 seconds between polls
  private static final int JOB_POLL_MAX_ATTEMPTS = 60;   // max 5 minutes (60 × 5s)

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

    List<UserExternalAccountChanges> userRecordsToUpdate = getRecentlyChangedUsersOrThrow();
    if (userRecordsToUpdate.isEmpty()) {
      log.info("{}No users to synchronize", MAILJET);
      return;
    }

    SyncMetrics metrics = new SyncMetrics();
    List<Long> successfullyProcessedUserIds = new ArrayList<>();
    List<UserExternalAccountChanges> failedUsers = new ArrayList<>();

    List<UserExternalAccountChanges> usersToDelete = new ArrayList<>();
    List<UserExternalAccountChanges> usersToSync = new ArrayList<>();
    partitionUsersByType(userRecordsToUpdate, usersToDelete, usersToSync);

    processDeletions(usersToDelete, metrics, successfullyProcessedUserIds);
    processBulkSyncsWithJobTracking(usersToSync, metrics, successfullyProcessedUserIds, failedUsers);
    markSuccessfullyProcessedAsSynced(successfullyProcessedUserIds, metrics);
    logFailedUsers(failedUsers);

    logSyncSummary(metrics, userRecordsToUpdate.size());
  }

  /**
   * Retrieve recently changed users from database.
   * Extracted to reduce cognitive complexity.
   */
  private List<UserExternalAccountChanges> getRecentlyChangedUsersOrThrow()
      throws ExternalAccountSynchronisationException {
    try {
      List<UserExternalAccountChanges> users = database.getRecentlyChangedRecords();
      log.info("{}Found {} users to synchronize with Mailjet", MAILJET, users.size());
      return users;
    } catch (SegueDatabaseException e) {
      throw new ExternalAccountSynchronisationException("Failed to retrieve users for synchronization"
              + e.getMessage());
    }
  }

  /**
   * Partition users into deletions and syncs.
   * Extracted to reduce cognitive complexity.
   */
  private void partitionUsersByType(final List<UserExternalAccountChanges> userRecords,
                                     final List<UserExternalAccountChanges> usersToDelete,
                                     final List<UserExternalAccountChanges> usersToSync) {
    for (UserExternalAccountChanges userRecord : userRecords) {
      if (Boolean.TRUE.equals(userRecord.isDeleted()) && userRecord.getProviderUserId() != null) {
        usersToDelete.add(userRecord);
      } else {
        usersToSync.add(userRecord);
      }
    }
  }

  /**
   * Process user deletions with error handling and backoff.
   * Extracted to reduce cognitive complexity.
   */
  private void processDeletions(final List<UserExternalAccountChanges> usersToDelete, final SyncMetrics metrics,
                                 final List<Long> successfullyProcessedUserIds)
      throws ExternalAccountSynchronisationException {
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
  }

  /**
   * Process bulk syncs with job tracking, polling, and per-user recovery.
   * Groups users by subscription state, submits batches, polls for completion,
   * and handles errors by verifying users individually at Mailjet.
   */
  private void processBulkSyncsWithJobTracking(final List<UserExternalAccountChanges> usersToSync,
                                                final SyncMetrics metrics,
                                                final List<Long> successfullyProcessedUserIds,
                                                final List<UserExternalAccountChanges> failedUsers)
      throws ExternalAccountSynchronisationException {
    try {
      Map<SubscriptionGroup, List<UserExternalAccountChanges>> groupedUsers =
          groupUsersBySubscriptionState(usersToSync);

      List<BatchJob> submittedJobs = new ArrayList<>();
      for (Map.Entry<SubscriptionGroup, List<UserExternalAccountChanges>> entry : groupedUsers.entrySet()) {
        List<BatchJob> groupJobs = submitBatchesForGroup(entry.getKey(), entry.getValue());
        submittedJobs.addAll(groupJobs);
      }

      for (BatchJob batch : submittedJobs) {
        try {
          Optional<JobStatus> status = pollJobToCompletion(batch.jobId());
          if (status.isEmpty()) {
            log.warn("{}Job {} timed out. Treating all {} users as failed.", MAILJET, batch.jobId(), batch.users().size());
            failedUsers.addAll(batch.users());
            metrics.incrementUnexpectedError(batch.users().size());
            continue;
          }
          processCompletedJob(batch, status.get(), successfullyProcessedUserIds, failedUsers, metrics);
        } catch (ExternalAccountSynchronisationException e) {
          log.error("{}Job {} polling failed: {}", MAILJET, batch.jobId(), e.getMessage());
          throw e;
        }
      }
    } catch (ExternalAccountSynchronisationException e) {
      throw e;
    } catch (Exception e) {
      metrics.incrementUnexpectedError();
      log.error("{}Unexpected error during bulk sync", MAILJET, e);
    }
  }

  /**
   * Submit batches for a subscription group and return list of submitted jobs.
   */
  private List<BatchJob> submitBatchesForGroup(final SubscriptionGroup group,
                                                final List<UserExternalAccountChanges> groupUsers)
      throws ExternalAccountSynchronisationException {
    List<BatchJob> submittedJobs = new ArrayList<>();

    for (int i = 0; i < groupUsers.size(); i += BULK_BATCH_SIZE) {
      int endIndex = Math.min(i + BULK_BATCH_SIZE, groupUsers.size());
      List<UserExternalAccountChanges> batch = groupUsers.subList(i, endIndex);

      try {
        String jobId = mailjetApi.bulkSyncUsers(batch, group.newsAction, group.eventsAction);
        if (jobId != null && !jobId.trim().isEmpty()) {
          submittedJobs.add(new BatchJob(jobId, group, new ArrayList<>(batch)));
        } else {
          log.warn("{}Bulk sync returned null/empty job ID for {} users. Continuing.", MAILJET, batch.size());
        }
      } catch (MailjetRateLimitException e) {
        log.warn("{}Mailjet rate limit exceeded during batch submission.", MAILJET);
        throw new ExternalAccountSynchronisationException("Mailjet API rate limits exceeded: " + e.getMessage());
      } catch (MailjetClientCommunicationException e) {
        log.error("{}Communication error during batch submission for {} users.", MAILJET, batch.size(), e);
        throw new ExternalAccountSynchronisationException("Failed to connect to Mailjet: " + e.getMessage());
      } catch (MailjetException e) {
        log.error("{}Mailjet API error during batch submission of {} users. Continuing with next batch.",
            MAILJET, batch.size(), e);
      }
    }

    return submittedJobs;
  }

  /**
   * Poll a job until it completes or times out.
   * Returns Optional.empty() if job times out; otherwise returns the final JobStatus.
   * Fails fast on repeated rate limiting (2+ consecutive rate limits).
   */
  private Optional<JobStatus> pollJobToCompletion(final String jobId)
      throws ExternalAccountSynchronisationException {
    int consecutiveRateLimits = 0;

    for (int attempt = 0; attempt < JOB_POLL_MAX_ATTEMPTS; attempt++) {
      try {
        JobStatus status = mailjetApi.getBulkJobStatus(jobId);
        consecutiveRateLimits = 0;  // Reset on successful poll

        if (status.isComplete() || status.hasFailed()) {
          log.debug("{}Job {} completed with status: {}", MAILJET, jobId, status.status());
          return Optional.of(status);
        }
        log.debug("{}Job {} in progress (attempt {}/{}). Processed: {}, Errors: {}",
            MAILJET, jobId, attempt + 1, JOB_POLL_MAX_ATTEMPTS, status.processed(), status.errors());

        Thread.sleep(JOB_POLL_INTERVAL_MS);
      } catch (MailjetRateLimitException e) {
        consecutiveRateLimits++;
        log.warn("{}Rate limit during job {} polling (attempt {}). Consecutive limits: {}",
            MAILJET, jobId, attempt + 1, consecutiveRateLimits);

        if (consecutiveRateLimits >= 2) {
          log.error("{}Job {} hit rate limit {} times. Failing fast to respect API limits.",
              MAILJET, jobId, consecutiveRateLimits);
          throw new ExternalAccountSynchronisationException(
              "Mailjet API rate limit exceeded during job polling for job " + jobId);
        }
      } catch (MailjetException e) {
        consecutiveRateLimits = 0;  // Reset on non-rate-limit errors
        log.warn("{}Error polling job {}: {}. Continuing with next attempt.", MAILJET, jobId, e.getMessage());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("{}Polling interrupted for job {}.", MAILJET, jobId);
        return Optional.empty();
      }
    }

    log.warn("{}Job {} timed out after {} attempts.", MAILJET, jobId, JOB_POLL_MAX_ATTEMPTS);
    return Optional.empty();
  }

  /**
   * Process a completed job: happy path (0 errors) or error path (recover per-user).
   */
  private void processCompletedJob(final BatchJob batch, final JobStatus status,
                                    final List<Long> successfullyProcessedUserIds,
                                    final List<UserExternalAccountChanges> failedUsers,
                                    final SyncMetrics metrics) {
    if (status.errors() == 0) {
      log.info("{}Job {} completed successfully with {} users (inserted: {}, updated: {}, unchanged: {})",
          MAILJET, batch.jobId(), batch.users().size(), status.inserted(), status.updated(), status.unchanged());
      for (UserExternalAccountChanges user : batch.users()) {
        successfullyProcessedUserIds.add(user.getUserId());
        metrics.incrementSuccess();
      }
    } else {
      log.warn("{}Job {} completed with {} errors. Recovering per-user.",
          MAILJET, batch.jobId(), status.errors());
      recoverUsersFromFailedJob(batch, successfullyProcessedUserIds, failedUsers, metrics);
    }
  }

  /**
   * Recover users from a failed job by querying each user individually at Mailjet.
   * Verifies that user data (name, role, verification status, stage) was correctly synced.
   */
  private void recoverUsersFromFailedJob(final BatchJob batch,
                                          final List<Long> successfullyProcessedUserIds,
                                          final List<UserExternalAccountChanges> failedUsers,
                                          final SyncMetrics metrics) {
    for (UserExternalAccountChanges user : batch.users()) {
      try {
        JSONObject mailjetContact = mailjetApi.getAccountByIdOrEmail(user.getAccountEmail());
        if (mailjetContact != null && isUserDataCorrectInMailjet(mailjetContact, user)) {
          log.debug("{}User ID {} verified in Mailjet after job error.", MAILJET, user.getUserId());
          successfullyProcessedUserIds.add(user.getUserId());
          metrics.incrementSuccess();
        } else {
          log.warn("{}User ID {} ({}) data mismatch or not found in Mailjet after job error.",
              MAILJET, user.getUserId(), user.getAccountEmail());
          failedUsers.add(user);
          metrics.incrementMailjetError();
        }
      } catch (MailjetException e) {
        log.warn("{}Failed to verify user ID {} ({}) at Mailjet: {}",
            MAILJET, user.getUserId(), user.getAccountEmail(), e.getMessage());
        failedUsers.add(user);
        metrics.incrementMailjetError();
      }
    }
  }

  /**
   * Verify that user data in Mailjet matches what we expect.
   * Checks: givenName, role, emailVerificationStatus, stage.
   */
  private boolean isUserDataCorrectInMailjet(final JSONObject mailjetContact,
                                              final UserExternalAccountChanges user) {
    try {
      String mailjetName = mailjetContact.optString("Name", "");
      String expectedName = user.getGivenName() != null ? user.getGivenName() : "";

      JSONObject properties = mailjetContact.optJSONObject("Properties");
      if (properties == null) {
        log.debug("{}No properties found in Mailjet contact for user ID {}.", MAILJET, user.getUserId());
        return false;
      }

      String mailjetRole = properties.optString("role", "");
      String expectedRole = user.getRole().toString();

      String mailjetVerification = properties.optString("verification_status", "");
      String expectedVerification = user.getEmailVerificationStatus().toString();

      String mailjetStage = properties.optString("stage", "");
      String expectedStage = user.getStage() != null ? user.getStage() : "unknown";

      boolean nameMatches = mailjetName.equals(expectedName);
      boolean roleMatches = mailjetRole.equals(expectedRole);
      boolean verificationMatches = mailjetVerification.equals(expectedVerification);
      boolean stageMatches = mailjetStage.equals(expectedStage);

      if (!nameMatches || !roleMatches || !verificationMatches || !stageMatches) {
        if (log.isDebugEnabled()) {
          log.debug("{}User ID {} data mismatch: name ({}/{}) role ({}/{}) verification ({}/{}) stage ({}/{})",
              MAILJET, user.getUserId(),
              mailjetName, expectedName,
              mailjetRole, expectedRole,
              mailjetVerification, expectedVerification,
              mailjetStage, expectedStage);
        }
        return false;
      }

      return true;

    } catch (JSONException e) {
      log.warn("{}JSON error checking user data for user ID {}: {}", MAILJET, user.getUserId(), e.getMessage());
      return false;
    }
  }

  /**
   * Log failed users that could not be synced to Mailjet.
   */
  private void logFailedUsers(final List<UserExternalAccountChanges> failedUsers) {
    if (failedUsers.isEmpty()) {
      return;
    }
    log.warn("{}=== {} users failed Mailjet sync ===", MAILJET, failedUsers.size());
    for (UserExternalAccountChanges user : failedUsers) {
      log.warn("{}Failed user - ID: {}, email: {}", MAILJET, user.getUserId(), user.getAccountEmail());
    }
  }

  /**
   * Mark successfully processed users as synced in database.
   * Extracted to reduce cognitive complexity.
   */
  private void markSuccessfullyProcessedAsSynced(final List<Long> successfullyProcessedUserIds,
                                                  final SyncMetrics metrics) {
    try {
      if (!successfullyProcessedUserIds.isEmpty()) {
        database.batchMarkAsSynced(successfullyProcessedUserIds);
      }
    } catch (SegueDatabaseException e) {
      metrics.incrementDatabaseError();
      log.error("{}Database error marking {} users as synced", MAILJET, successfullyProcessedUserIds.size(), e);
    }
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

    void incrementUnexpectedError(int count) {
      unexpectedErrorCount += count;
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

  /**
   * Record representing a submitted batch job with its users.
   */
  private record BatchJob(
      String jobId,
      SubscriptionGroup group,
      List<UserExternalAccountChanges> users
  ) {}
}