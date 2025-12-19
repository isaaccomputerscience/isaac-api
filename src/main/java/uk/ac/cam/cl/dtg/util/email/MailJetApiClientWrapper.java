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

package uk.ac.cam.cl.dtg.util.email;

import com.google.inject.Inject;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetClientCommunicationException;
import com.mailjet.client.errors.MailjetClientRequestException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Contact;
import com.mailjet.client.resource.ContactManagecontactslists;
import com.mailjet.client.resource.Contactdata;
import com.mailjet.client.resource.Contacts;
import com.mailjet.client.resource.ContactslistImportList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailJetApiClientWrapper {

  private static final Logger log = LoggerFactory.getLogger(MailJetApiClientWrapper.class);
  private static final long DEFAULT_RATE_LIMIT_DELAY_MS = 2000; // 2 seconds between API calls

  private final MailjetClient mailjetClient;
  private final String newsListId;
  private final String eventsListId;
  private final String legalListId;
  private final long rateLimitDelayMs;

  // Track last API call time for rate limiting
  private long lastApiCallTime = 0;

  /**
   * Wrapper for MailjetClient class.
   *
   * @param mailjetApiKey       - MailJet API Key
   * @param mailjetApiSecret    - MailJet API Client Secret
   * @param mailjetNewsListId   - MailJet list ID for NEWS_AND_UPDATES
   * @param mailjetEventsListId - MailJet list ID for EVENTS
   * @param mailjetLegalListId  - MailJet list ID for legal notices (all users)
   */
  @Inject
  public MailJetApiClientWrapper(final String mailjetApiKey, final String mailjetApiSecret,
                                 final String mailjetNewsListId, final String mailjetEventsListId,
                                 final String mailjetLegalListId) {
    this(mailjetApiKey, mailjetApiSecret, mailjetNewsListId, mailjetEventsListId,
            mailjetLegalListId, DEFAULT_RATE_LIMIT_DELAY_MS);
  }

  /**
   * Wrapper for MailjetClient class with configurable rate limiting.
   *
   * @param mailjetApiKey         - MailJet API Key
   * @param mailjetApiSecret      - MailJet API Client Secret
   * @param mailjetNewsListId     - MailJet list ID for NEWS_AND_UPDATES
   * @param mailjetEventsListId   - MailJet list ID for EVENTS
   * @param mailjetLegalListId    - MailJet list ID for legal notices (all users)
   * @param rateLimitDelayMs      - Delay in milliseconds between API calls (default: 2000ms)
   */
  public MailJetApiClientWrapper(final String mailjetApiKey, final String mailjetApiSecret,
                                 final String mailjetNewsListId, final String mailjetEventsListId,
                                 final String mailjetLegalListId, final long rateLimitDelayMs) {

    if (mailjetApiKey == null || mailjetApiSecret == null) {
      throw new IllegalArgumentException("Mailjet API credentials cannot be null");
    }

    ClientOptions options = ClientOptions.builder()
            .apiKey(mailjetApiKey)
            .apiSecretKey(mailjetApiSecret)
            .build();

    this.mailjetClient = new MailjetClient(options);
    this.newsListId = mailjetNewsListId;
    this.eventsListId = mailjetEventsListId;
    this.legalListId = mailjetLegalListId;
    this.rateLimitDelayMs = rateLimitDelayMs;

    log.info("MAILJETT - MailJet API wrapper initialized with list IDs - News: {}, Events: {}, Legal: {}",
            newsListId, eventsListId, legalListId);
    log.info("MAILJETT - Rate limiting enabled: {}ms delay between API calls", rateLimitDelayMs);
  }

  /**
   * Get user details for an existing MailJet account.
   *
   * @param mailjetIdOrEmail - email address or MailJet user ID
   * @return JSONObject of the MailJet user, or null if not found
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public JSONObject getAccountByIdOrEmail(final String mailjetIdOrEmail) throws MailjetException {
    if (mailjetIdOrEmail == null || mailjetIdOrEmail.trim().isEmpty()) {
      log.info("MAILJETT - Attempted to get account with null/empty identifier");
      return null;
    }

    waitForRateLimit(); // Apply rate limiting

    try {
      log.info("MAILJETT - Fetching Mailjet account: {}", mailjetIdOrEmail);
      MailjetRequest request = new MailjetRequest(Contact.resource, mailjetIdOrEmail);
      log.info("MAILJETT - request: {}", request.getBody());
      MailjetResponse response = mailjetClient.get(request);

      log.info("MAILJETT - response: {}", response.getRawResponseContent());

      if (response.getStatus() == 404) {
        log.info("MAILJETT - Mailjet account not found: {}", mailjetIdOrEmail);
        return null;
      }

      if (response.getStatus() != 200) {
        log.warn("MAILJETT - Unexpected Mailjet response status {} when fetching account: {}",
                response.getStatus(), mailjetIdOrEmail);
        throw new MailjetException("Unexpected response status: " + response.getStatus());
      }

      JSONArray responseData = response.getData();
      if (response.getTotal() == 1 && !responseData.isEmpty()) {
        log.info("MAILJETT - Successfully retrieved Mailjet account: {}", mailjetIdOrEmail);
        return responseData.getJSONObject(0);
      }

      log.info("MAILJETT - Mailjet account not found (total={}): {}", response.getTotal(), mailjetIdOrEmail);
      return null;

    } catch (MailjetException e) {
      // Check if it's a 404 "Object not found" error
      if (e.getMessage() != null &&
              (e.getMessage().contains("404") ||
                      e.getMessage().toLowerCase().contains("not found") ||
                      e.getMessage().toLowerCase().contains("object not found"))) {
        log.info("MAILJETT - Mailjet account not found (404): {}. Error: {}", mailjetIdOrEmail, e.getMessage());
        return null;  // Treat 404 as "not found", not an error
      }

      // Check if it's a timeout/communication issue
      if (e.getMessage() != null &&
              (e.getMessage().toLowerCase().contains("timeout") ||
                      e.getMessage().toLowerCase().contains("connection"))) {
        log.error("MAILJETT - Communication error fetching Mailjet account: {}", mailjetIdOrEmail, e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }

      log.error("MAILJETT - Error fetching Mailjet account: {}", mailjetIdOrEmail, e);
      throw e;
    }
  }

  /**
   * Perform an asynchronous GDPR-compliant deletion of a MailJet user.
   *
   * @param mailjetId - MailJet user ID
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public void permanentlyDeleteAccountById(final String mailjetId) throws MailjetException {
    if (mailjetId == null || mailjetId.trim().isEmpty()) {
      throw new IllegalArgumentException("Mailjet ID cannot be null or empty");
    }

    waitForRateLimit(); // Apply rate limiting

    try {
      log.info("MAILJETT - Deleting Mailjet account: {}", mailjetId);

      MailjetRequest request = new MailjetRequest(Contacts.resource, mailjetId);

      log.info("MAILJETT - request: {}", request.getBody());

      MailjetResponse response = mailjetClient.delete(request);

      log.info("MAILJETT - response: {}", response.getRawResponseContent());

      if (response.getStatus() == 204 || response.getStatus() == 200) {
        log.info("MAILJETT - Successfully deleted Mailjet account: {}", mailjetId);
      } else if (response.getStatus() == 404) {
        log.warn("MAILJETT - Attempted to delete non-existent Mailjet account: {}", mailjetId);
        // Don't throw - account is already gone
      } else {
        log.error("MAILJETT - Unexpected response status {} when deleting Mailjet account: {}",
                response.getStatus(), mailjetId);
        throw new MailjetException("Failed to delete account. Status: " + response.getStatus());
      }

    } catch (MailjetException e) {
      // Check if it's a 404 - account already deleted
      if (e.getMessage() != null &&
              (e.getMessage().contains("404") ||
                      e.getMessage().toLowerCase().contains("not found") ||
                      e.getMessage().toLowerCase().contains("object not found"))) {
        log.warn("MAILJETT - Mailjet account already deleted or not found: {}. Treating as success.", mailjetId);
        return;  // Already deleted - treat as success
      }

      // Check if it's a timeout/communication issue
      if (e.getMessage() != null &&
              (e.getMessage().toLowerCase().contains("timeout") ||
                      e.getMessage().toLowerCase().contains("connection"))) {
        log.error("MAILJETT - Communication error deleting Mailjet account: {}", mailjetId, e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }
      log.error("MAILJETT - Error deleting Mailjet account: {}", mailjetId, e);
      throw e;
    }
  }

  /**
   * Add a new user to MailJet.
   * <br>
   * If the user already exists, find by email as a fallback to ensure idempotence and better error recovery.
   *
   * @param email - email address
   * @return the MailJet user ID, or null on failure
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public String addNewUserOrGetUserIfExists(final String email) throws MailjetException {
    if (email == null || email.trim().isEmpty()) {
      log.warn("MAILJETT - Attempted to create Mailjet account with null/empty email");
      return null;
    }

    String normalizedEmail = email.trim().toLowerCase();

    waitForRateLimit(); // Apply rate limiting

    try {
      log.info("MAILJETT - Creating Mailjet account for email: {}", maskEmail(normalizedEmail));

      MailjetRequest request = new MailjetRequest(Contact.resource)
              .property(Contact.EMAIL, normalizedEmail);
      log.info("MAILJETT - request: {}", request.getBody());
      MailjetResponse response = mailjetClient.post(request);

      log.info("MAILJETT - response: {}", response.getRawResponseContent());

      if (response.getStatus() == 201 || response.getStatus() == 200) {
        JSONObject responseData = response.getData().getJSONObject(0);
        log.info("MAILJETT - responseData : {}", responseData.toString());
        String mailjetId = String.valueOf(responseData.get("ID"));
        log.info("MAILJETT - Successfully created Mailjet account {} for email: {}",
                mailjetId, maskEmail(normalizedEmail));
        return mailjetId;
      }

      log.error("MAILJETT - Unexpected response status {} when creating Mailjet account for: {}",
              response.getStatus(), maskEmail(normalizedEmail));
      throw new MailjetException("Failed to create account. Status: " + response.getStatus());

    } catch (MailjetClientRequestException e) {
      // Check if user already exists
      if (e.getMessage() != null && e.getMessage().toLowerCase().contains("already exists")) {
        log.info("MAILJETT - User already exists in Mailjet for email: {}. Fetching existing account.",
                maskEmail(normalizedEmail));

        try {
          JSONObject existingAccount = getAccountByIdOrEmail(normalizedEmail);
          if (existingAccount != null) {
            String mailjetId = String.valueOf(existingAccount.get("ID"));
            log.info("MAILJETT - Retrieved existing Mailjet account {} for email: {}",
                    mailjetId, maskEmail(normalizedEmail));
            return mailjetId;
          } else {
            log.error("MAILJETT - User reported as existing but couldn't fetch account for: {}",
                    maskEmail(normalizedEmail));
            throw new MailjetException("Account exists but couldn't be retrieved");
          }
        } catch (JSONException je) {
          log.error("MAILJETT - JSON parsing error when retrieving existing account for: {}",
                  maskEmail(normalizedEmail), je);
          throw new MailjetException("Failed to parse existing account data", je);
        }
      } else {
        log.error("MAILJETT - Failed to create Mailjet account for: {}. Error: {}",
                maskEmail(normalizedEmail), e.getMessage(), e);
        throw new MailjetException("Failed to create account: " + e.getMessage(), e);
      }

    } catch (MailjetException e) {
      // Check if it's a timeout/communication issue
      if (e.getMessage() != null &&
              (e.getMessage().toLowerCase().contains("timeout") ||
                      e.getMessage().toLowerCase().contains("connection"))) {
        log.error("MAILJETT - Communication error creating Mailjet account for: {}", maskEmail(normalizedEmail), e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }
      log.error("MAILJETT - Error creating Mailjet account for: {}", maskEmail(normalizedEmail), e);
      throw e;

    } catch (JSONException e) {
      log.error("MAILJETT - JSON parsing error when creating account for: {}", maskEmail(normalizedEmail), e);
      throw new MailjetException("Failed to parse Mailjet response", e);
    }
  }

  /**
   * Update user details for an existing MailJet account.
   *
   * @param mailjetId               - MailJet user ID
   * @param firstName               - first name of user for contact details
   * @param role                    - role of user for contact details
   * @param emailVerificationStatus - verification status of user for contact details
   * @param stage                   - stages of GCSE or A Level
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public void updateUserProperties(final String mailjetId, final String firstName,
                                   final String role, final String emailVerificationStatus,
                                   final String stage) throws MailjetException {
    if (mailjetId == null || mailjetId.trim().isEmpty()) {
      throw new IllegalArgumentException("Mailjet ID cannot be null or empty");
    }

    waitForRateLimit(); // Apply rate limiting

    try {
      log.info("MAILJETT - Updating properties for Mailjet account: {} (role={}, stage={}, status={})",
              mailjetId, role, stage, emailVerificationStatus);

      MailjetRequest request = new MailjetRequest(Contactdata.resource, mailjetId)
              .property(Contactdata.DATA, new JSONArray()
                      .put(new JSONObject().put("Name", "firstname").put("value", firstName != null ? firstName : ""))
                      .put(new JSONObject().put("Name", "role").put("value", role != null ? role : ""))
                      .put(new JSONObject().put("Name", "verification_status")
                              .put("value", emailVerificationStatus != null ? emailVerificationStatus : ""))
                      .put(new JSONObject().put("Name", "stage").put("value", stage != null ? stage : "unknown"))
              );

      log.info("MAILJETT - request: {}", request.getBody());

      MailjetResponse response = mailjetClient.put(request);

      log.info("MAILJETT - response: {}", response.getRawResponseContent());

      if (response.getStatus() == 200 && response.getTotal() == 1) {
        log.info("MAILJETT - Successfully updated properties for Mailjet account: {}", mailjetId);
      } else {
        log.error("MAILJETT - Failed to update properties for Mailjet account: {}. Status: {}, Total: {}",
                mailjetId, response.getStatus(), response.getTotal());
        throw new MailjetException(
                String.format("Failed to update user properties. Status: %d, Total: %d",
                        response.getStatus(), response.getTotal()));
      }

    } catch (MailjetException e) {
      // Check if it's a 404 - contact not found
      if (e.getMessage() != null &&
              (e.getMessage().contains("404") ||
                      e.getMessage().toLowerCase().contains("not found") ||
                      e.getMessage().toLowerCase().contains("object not found"))) {
        log.error("MAILJETT - Mailjet contact not found when updating properties: {}. The contact may have been deleted.", mailjetId);
        throw new MailjetException("Contact not found (404) when updating properties: " + mailjetId, e);
      }

      // Check if it's a timeout/communication issue
      if (e.getMessage() != null &&
              (e.getMessage().toLowerCase().contains("timeout") ||
                      e.getMessage().toLowerCase().contains("connection"))) {
        log.error("MAILJETT - Communication error updating properties for Mailjet account: {}", mailjetId, e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }
      log.error("MAILJETT - Error updating properties for Mailjet account: {}", mailjetId, e);
      throw e;
    }
  }

  /**
   * Update user list subscriptions for an existing MailJet account.
   *
   * @param mailjetId    - MailJet user ID
   * @param newsEmails   - subscription action to take for news emails
   * @param eventsEmails - subscription action to take for events emails
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public void updateUserSubscriptions(final String mailjetId,
                                      final MailJetSubscriptionAction newsEmails,
                                      final MailJetSubscriptionAction eventsEmails)
          throws MailjetException {

    if (mailjetId == null || mailjetId.trim().isEmpty()) {
      throw new IllegalArgumentException("Mailjet ID cannot be null or empty");
    }

    if (newsEmails == null || eventsEmails == null) {
      throw new IllegalArgumentException("Subscription actions cannot be null");
    }

    waitForRateLimit(); // Apply rate limiting

    try {
      log.info("MAILJETT - Updating subscriptions for Mailjet account: {} (news={}, events={})",
              mailjetId, newsEmails, eventsEmails);

      MailjetRequest request = new MailjetRequest(ContactManagecontactslists.resource, mailjetId)
              .property(ContactManagecontactslists.CONTACTSLISTS, new JSONArray()
                      .put(new JSONObject()
                              .put(ContactslistImportList.LISTID, legalListId)
                              .put(ContactslistImportList.ACTION, MailJetSubscriptionAction.FORCE_SUBSCRIBE.getValue()))
                      .put(new JSONObject()
                              .put(ContactslistImportList.LISTID, newsListId)
                              .put(ContactslistImportList.ACTION, newsEmails.getValue()))
                      .put(new JSONObject()
                              .put(ContactslistImportList.LISTID, eventsListId)
                              .put(ContactslistImportList.ACTION, eventsEmails.getValue()))
              );

      log.info("MAILJETT - request: {}", request.getBody());

      MailjetResponse response = mailjetClient.post(request);

      log.info("MAILJETT - response: {}", response.getRawResponseContent());

      if (response.getStatus() == 201 && response.getTotal() == 1) {
        log.info("MAILJETT - Successfully updated subscriptions for Mailjet account: {}", mailjetId);
      } else {
        log.error("MAILJETT - Failed to update subscriptions for Mailjet account: {}. Status: {}, Total: {}",
                mailjetId, response.getStatus(), response.getTotal());
        throw new MailjetException(
                String.format("Failed to update user subscriptions. Status: %d, Total: %d",
                        response.getStatus(), response.getTotal()));
      }

    } catch (MailjetException e) {
      // Check if it's a 404 - contact not found
      if (e.getMessage() != null &&
              (e.getMessage().contains("404") ||
                      e.getMessage().toLowerCase().contains("not found") ||
                      e.getMessage().toLowerCase().contains("object not found"))) {
        log.error("MAILJETT - Mailjet contact not found when updating subscriptions: {}. The contact may have been deleted.", mailjetId);
        throw new MailjetException("Contact not found (404) when updating subscriptions: " + mailjetId, e);
      }

      // Check if it's a timeout/communication issue
      if (e.getMessage() != null &&
              (e.getMessage().toLowerCase().contains("timeout") ||
                      e.getMessage().toLowerCase().contains("connection"))) {
        log.error("MAILJETT - Communication error updating subscriptions for Mailjet account: {}", mailjetId, e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }
      log.error("MAILJETT - Error updating subscriptions for Mailjet account: {}", mailjetId, e);
      throw e;
    }
  }

  /**
   * Mask email for logging purposes.
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
   * Wait for rate limiting before making an API call.
   * Ensures minimum delay between consecutive API calls to avoid rate limits.
   *
   * This method is synchronized to ensure thread-safety when multiple threads
   * might be using the same MailJetApiClientWrapper instance.
   */
  private synchronized void waitForRateLimit() {
    long currentTime = System.currentTimeMillis();
    long timeSinceLastCall = currentTime - lastApiCallTime;

    if (timeSinceLastCall < rateLimitDelayMs && lastApiCallTime > 0) {
      long waitTime = rateLimitDelayMs - timeSinceLastCall;
      log.info("MAILJETT - Rate limiting: waiting {}ms before next API call", waitTime);

      try {
        Thread.sleep(waitTime);
      } catch (InterruptedException e) {
        log.warn("MAILJETT - Rate limit wait interrupted", e);
        Thread.currentThread().interrupt();
      }
    }

    lastApiCallTime = System.currentTimeMillis();
  }
}