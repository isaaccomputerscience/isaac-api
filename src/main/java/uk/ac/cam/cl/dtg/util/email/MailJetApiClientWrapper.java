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

  private final MailjetClient mailjetClient;
  private final String newsListId;
  private final String eventsListId;
  private final String legalListId;

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

    if (mailjetApiKey == null || mailjetApiSecret == null) {
      throw new IllegalArgumentException("Mailjet API credentials cannot be null");
    }

    ClientOptions options = ClientOptions.builder().apiKey(mailjetApiKey).apiSecretKey(mailjetApiSecret).build();

    this.mailjetClient = new MailjetClient(options);
    this.newsListId = mailjetNewsListId;
    this.eventsListId = mailjetEventsListId;
    this.legalListId = mailjetLegalListId;
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
      log.debug("Attempted to get account with null/empty identifier");
      return null;
    }

    try {
      MailjetRequest request = new MailjetRequest(Contact.resource, mailjetIdOrEmail);
      MailjetResponse response = mailjetClient.get(request);

      if (response.getStatus() == 404) {
        return null;
      }

      if (response.getStatus() != 200) {
        log.warn("Unexpected Mailjet response status {} when fetching account", response.getStatus());
        throw new MailjetException("Unexpected response status: " + response.getStatus());
      }

      JSONArray responseData = response.getData();
      if (response.getTotal() == 1 && !responseData.isEmpty()) {
        return responseData.getJSONObject(0);
      }

      return null;

    } catch (MailjetException e) {
      if (isNotFoundException(e)) {
        return null;
      }

      if (isCommunicationException(e)) {
        log.error("Communication error fetching Mailjet account", e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }

      log.error("Error fetching Mailjet account", e);
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

    try {
      MailjetRequest request = new MailjetRequest(Contacts.resource, mailjetId);
      MailjetResponse response = mailjetClient.delete(request);

      if (response.getStatus() == 204 || response.getStatus() == 200) {
        log.info("Successfully deleted Mailjet account: {}", mailjetId);
      } else if (response.getStatus() == 404) {
        log.debug("Attempted to delete non-existent Mailjet account: {}", mailjetId);
      } else {
        log.error("Unexpected response status {} when deleting Mailjet account", response.getStatus());
        throw new MailjetException("Failed to delete account. Status: " + response.getStatus());
      }

    } catch (MailjetException e) {
      if (isNotFoundException(e)) {
        log.debug("Mailjet account already deleted or not found: {}", mailjetId);
        return;
      }

      if (isCommunicationException(e)) {
        log.error("Communication error deleting Mailjet account: {}", mailjetId, e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }

      log.error("Error deleting Mailjet account: {}", mailjetId, e);
      throw e;
    }
  }

  /**
   * Add a new user to MailJet.
   * <br>
   * If the user already exists, find by email as a fallback to ensure idempotence.
   *
   * @param email - email address
   * @return the MailJet user ID, or null on failure
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public String addNewUserOrGetUserIfExists(final String email) throws MailjetException {
    if (email == null || email.trim().isEmpty()) {
      log.warn("Attempted to create Mailjet account with null/empty email");
      return null;
    }

    String normalizedEmail = email.trim().toLowerCase();

    try {
      MailjetRequest request = new MailjetRequest(Contact.resource).property(Contact.EMAIL, normalizedEmail);
      MailjetResponse response = mailjetClient.post(request);

      if (response.getStatus() == 201 || response.getStatus() == 200) {
        JSONObject responseData = response.getData().getJSONObject(0);
        String mailjetId = String.valueOf(responseData.get("ID"));
        log.info("Successfully created Mailjet account: {}", mailjetId);
        return mailjetId;
      }

      log.error("Unexpected response status {} when creating Mailjet account", response.getStatus());
      throw new MailjetException("Failed to create account. Status: " + response.getStatus());

    } catch (MailjetClientRequestException e) {
      if (e.getMessage() != null && e.getMessage().toLowerCase().contains("already exists")) {
        log.debug("User already exists in Mailjet, fetching existing account");

        try {
          JSONObject existingAccount = getAccountByIdOrEmail(normalizedEmail);
          if (existingAccount != null) {
            String mailjetId = String.valueOf(existingAccount.get("ID"));
            log.info("Retrieved existing Mailjet account: {}", mailjetId);
            return mailjetId;
          } else {
            log.error("User reported as existing but couldn't fetch account");
            throw new MailjetException("Account exists but couldn't be retrieved");
          }
        } catch (JSONException je) {
          log.error("JSON parsing error when retrieving existing account", je);
          throw new MailjetException("Failed to parse existing account data", je);
        }
      } else {
        log.error("Failed to create Mailjet account: {}", e.getMessage(), e);
        throw new MailjetException("Failed to create account: " + e.getMessage(), e);
      }

    } catch (MailjetException e) {
      if (isCommunicationException(e)) {
        log.error("Communication error creating Mailjet account", e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }

      log.error("Error creating Mailjet account", e);
      throw e;

    } catch (JSONException e) {
      log.error("JSON parsing error when creating account", e);
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
  public void updateUserProperties(final String mailjetId, final String firstName, final String role,
                                   final String emailVerificationStatus, final String stage) throws MailjetException {
    if (mailjetId == null || mailjetId.trim().isEmpty()) {
      throw new IllegalArgumentException("Mailjet ID cannot be null or empty");
    }

    try {
      MailjetRequest request = new MailjetRequest(Contactdata.resource, mailjetId).property(Contactdata.DATA,
          new JSONArray().put(
                  new JSONObject().put("Name", "firstname").put("value", firstName != null ? firstName : ""))
              .put(new JSONObject().put("Name", "role").put("value", role != null ? role : "")).put(
                  new JSONObject().put("Name", "verification_status")
                      .put("value", emailVerificationStatus != null ? emailVerificationStatus : ""))
              .put(new JSONObject().put("Name", "stage").put("value", stage != null ? stage : "unknown")));

      MailjetResponse response = mailjetClient.put(request);

      if (response.getStatus() == 200 && response.getTotal() == 1) {
        log.debug("Successfully updated properties for Mailjet account: {}", mailjetId);
      } else {
        log.error("Failed to update properties for Mailjet account: {}. Status: {}, Total: {}", mailjetId,
            response.getStatus(), response.getTotal());
        throw new MailjetException(
            String.format("Failed to update user properties. Status: %d, Total: %d", response.getStatus(),
                response.getTotal()));
      }

    } catch (MailjetException e) {
      if (isNotFoundException(e)) {
        log.error("Mailjet contact not found when updating properties: {}. Contact may have been deleted", mailjetId);
        throw new MailjetException("Contact not found (404) when updating properties: " + mailjetId, e);
      }

      if (isCommunicationException(e)) {
        log.error("Communication error updating properties for Mailjet account: {}", mailjetId, e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }

      log.error("Error updating properties for Mailjet account: {}", mailjetId, e);
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
  public void updateUserSubscriptions(final String mailjetId, final MailJetSubscriptionAction newsEmails,
                                      final MailJetSubscriptionAction eventsEmails) throws MailjetException {

    if (mailjetId == null || mailjetId.trim().isEmpty()) {
      throw new IllegalArgumentException("Mailjet ID cannot be null or empty");
    }

    if (newsEmails == null || eventsEmails == null) {
      throw new IllegalArgumentException("Subscription actions cannot be null");
    }

    try {
      MailjetRequest request = new MailjetRequest(ContactManagecontactslists.resource, mailjetId).property(
          ContactManagecontactslists.CONTACTSLISTS, new JSONArray().put(
              new JSONObject().put(ContactslistImportList.LISTID, legalListId)
                  .put(ContactslistImportList.ACTION, MailJetSubscriptionAction.FORCE_SUBSCRIBE.getValue())).put(
                    new JSONObject().put(ContactslistImportList.LISTID, newsListId)
                  .put(ContactslistImportList.ACTION, newsEmails.getValue())).put(
                    new JSONObject().put(ContactslistImportList.LISTID, eventsListId)
                  .put(ContactslistImportList.ACTION, eventsEmails.getValue())));

      MailjetResponse response = mailjetClient.post(request);

      if (response.getStatus() == 201 && response.getTotal() == 1) {
        log.debug("Successfully updated subscriptions for Mailjet account: {}", mailjetId);
      } else {
        log.error("Failed to update subscriptions for Mailjet account: {}. Status: {}, Total: {}", mailjetId,
            response.getStatus(), response.getTotal());
        throw new MailjetException(
            String.format("Failed to update user subscriptions. Status: %d, Total: %d", response.getStatus(),
                response.getTotal()));
      }

    } catch (MailjetException e) {
      if (isNotFoundException(e)) {
        log.error("Mailjet contact not found when updating subscriptions: {}. Contact may have been deleted",
            mailjetId);
        throw new MailjetException("Contact not found (404) when updating subscriptions: " + mailjetId, e);
      }

      if (isCommunicationException(e)) {
        log.error("Communication error updating subscriptions for Mailjet account: {}", mailjetId, e);
        throw new MailjetClientCommunicationException("Failed to communicate with Mailjet", e);
      }

      log.error("Error updating subscriptions for Mailjet account: {}", mailjetId, e);
      throw e;
    }
  }

  /**
   * Check if exception is a 404 not found error.
   */
  private boolean isNotFoundException(MailjetException e) {
    if (e.getMessage() == null) {
      return false;
    }
    String msg = e.getMessage().toLowerCase();
    return msg.contains("404") || msg.contains("not found") || msg.contains("object not found");
  }

  /**
   * Check if exception is a communication/timeout error.
   */
  private boolean isCommunicationException(MailjetException e) {
    if (e.getMessage() == null) {
      return false;
    }
    String msg = e.getMessage().toLowerCase();
    return msg.contains("timeout") || msg.contains("connection");
  }
}
