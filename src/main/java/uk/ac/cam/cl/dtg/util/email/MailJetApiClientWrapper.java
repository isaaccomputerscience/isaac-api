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

import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
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
    ClientOptions options = ClientOptions.builder()
            .apiKey(mailjetApiKey)
            .apiSecretKey(mailjetApiSecret)
            .build();

    this.mailjetClient = new MailjetClient(options);
    this.newsListId = mailjetNewsListId;
    this.eventsListId = mailjetEventsListId;
    this.legalListId = mailjetLegalListId;

    log.info("MAILJET - MailJetApiClientWrapper initialized with list IDs - News: {}, Events: {}, Legal: {}",
            newsListId, eventsListId, legalListId);
  }

  /**
   * Get user details for an existing MailJet account.
   *
   * @param mailjetIdOrEmail - email address or MailJet user ID
   * @return JSONObject of the MailJet user
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public JSONObject getAccountByIdOrEmail(final String mailjetIdOrEmail) throws MailjetException {
    if (null == mailjetIdOrEmail) {
      log.warn("MAILJET - getAccountByIdOrEmail called with null mailjetIdOrEmail");
      return null;
    }

    log.debug("MAILJET - Getting Mailjet account for: {}", mailjetIdOrEmail);

    try {
      MailjetRequest request = new MailjetRequest(Contact.resource, mailjetIdOrEmail);
      MailjetResponse response = mailjetClient.get(request);

      log.debug("MAILJET - Mailjet GET Contact response - Status: {}, Total: {}",
              response.getStatus(), response.getTotal());

      JSONArray responseData = response.getData();
      if (response.getTotal() == 1) {
        JSONObject result = responseData.getJSONObject(0);
        log.debug("MAILJET - Found Mailjet contact: ID={}, Email={}",
                result.optInt("ID"), result.optString("Email"));
        return result;
      }

      log.warn("MAILJET - Mailjet account not found for: {}", mailjetIdOrEmail);
      return null;

    } catch (MailjetException e) {
      log.error("MAILJET - MailjetException in getAccountByIdOrEmail for: {}", mailjetIdOrEmail, e);
      throw e;
    } catch (Exception e) {
      log.error("MAILJET - Unexpected exception in getAccountByIdOrEmail for: {}", mailjetIdOrEmail, e);
      throw new MailjetException("Unexpected error getting account: " + e.getMessage(), e);
    }
  }

  /**
   * Perform an asynchronous GDPR-compliant deletion of a MailJet user.
   *
   * @param mailjetId - MailJet user ID
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public void permanentlyDeleteAccountById(final String mailjetId) throws MailjetException {
    requireNonNull(mailjetId);

    log.info("MAILJET - Permanently deleting Mailjet account: {}", mailjetId);

    try {
      MailjetRequest request = new MailjetRequest(Contacts.resource, mailjetId);
      MailjetResponse response = mailjetClient.delete(request);

      log.info("MAILJET - Mailjet DELETE response - Status: {}, mailjetId: {}",
              response.getStatus(), mailjetId);

      if (response.getStatus() != 204 && response.getStatus() != 200) {
        log.warn("MAILJET - Unexpected status code {} when deleting Mailjet account {}",
                response.getStatus(), mailjetId);
      }

    } catch (MailjetException e) {
      log.error("MAILJET - MailjetException deleting account: {}", mailjetId, e);
      throw e;
    } catch (Exception e) {
      log.error("MAILJET - Unexpected exception deleting account: {}", mailjetId, e);
      throw new MailjetException("Unexpected error deleting account: " + e.getMessage(), e);
    }
  }

  /**
   * Add a new user to MailJet
   * <br>
   * If the user already exists, find by email as a fallback to ensure idempotence and better error recovery.
   *
   * @param email - email address
   * @return the MailJet user ID
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public String addNewUserOrGetUserIfExists(final String email) throws MailjetException {
    if (null == email) {
      log.error("MAILJET - addNewUserOrGetUserIfExists called with null email");
      return null;
    }

    log.info("MAILJET - Creating Mailjet contact for email: {}", sanitiseEmailForLogging(email));

    try {
      MailjetRequest request = new MailjetRequest(Contact.resource).property(Contact.EMAIL, email);
      MailjetResponse response = mailjetClient.post(request);

      log.debug("MAILJET - Mailjet POST Contact response - Status: {}, Total: {}",
              response.getStatus(), response.getTotal());

      // Get MailJet ID out:
      JSONObject responseData = response.getData().getJSONObject(0);
      String mailjetId = Integer.toString(responseData.getInt("ID"));

      log.info("MAILJET - Successfully created Mailjet contact with ID: {} for email: {}",
              mailjetId, sanitiseEmailForLogging(email));

      return mailjetId;

    } catch (MailjetClientRequestException e) {
      log.warn("MAILJET - MailjetClientRequestException creating contact for email: {} - Message: {}",
              sanitiseEmailForLogging(email), e.getMessage());

      if (e.getMessage().contains("already exists")) {
        log.info("MAILJET - Contact already exists, attempting to retrieve existing contact for: {}",
                sanitiseEmailForLogging(email));

        try {
          JSONObject existingMailJetAccount = getAccountByIdOrEmail(email);
          if (existingMailJetAccount != null) {
            String existingId = Integer.toString(existingMailJetAccount.getInt("ID"));
            log.info("MAILJET - Found existing Mailjet contact with ID: {} for email: {}",
                    existingId, sanitiseEmailForLogging(email));
            return existingId;
          } else {
            log.error("MAILJET - Contact 'already exists' but getAccountByIdOrEmail returned null for: {}",
                    sanitiseEmailForLogging(email));
            throw new MailjetException("Contact exists but could not be retrieved");
          }
        } catch (JSONException jsonEx) {
          log.error("MAILJET - JSONException retrieving existing contact for: {}",
                  sanitiseEmailForLogging(email), jsonEx);
          throw new MailjetException("Failed to parse existing contact data", jsonEx);
        }
      } else {
        log.error("MAILJET - Failed to create user in MailJet with email: {} - Error: {}",
                sanitiseEmailForLogging(email), e.getMessage(), e);
        throw new MailjetException("Failed to create contact: " + e.getMessage(), e);
      }
    } catch (JSONException e) {
      log.error("MAILJET - JSONException creating user in MailJet with email: {}",
              sanitiseEmailForLogging(email), e);
      throw new MailjetException("Failed to parse Mailjet response", e);
    } catch (Exception e) {
      log.error("MAILJET - Unexpected exception creating user in MailJet with email: {}",
              sanitiseEmailForLogging(email), e);
      throw new MailjetException("Unexpected error creating contact: " + e.getMessage(), e);
    }
  }

  /**
   * Update user details for an existing MailJet account.
   *
   * @param mailjetId               - MailJet user ID
   * @param firstName               - first name of user for contact details
   * @param role                    - role of user for contact details
   * @param emailVerificationStatus - verification status of user for contact details
   * @param stage                   - stages of GCSE or ALevel
   * @throws MailjetException - if underlying MailjetClient throws an exception
   */
  public void updateUserProperties(final String mailjetId, final String firstName, final String role,
                                   final String emailVerificationStatus, String stage) throws MailjetException {
    requireNonNull(mailjetId);

    log.info("MAILJET - Updating properties for Mailjet ID: {}", mailjetId);
    log.debug("MAILJET - Properties - firstName: {}, role: {}, verificationStatus: {}, stage: {}",
            firstName, role, emailVerificationStatus, stage);

    try {
      JSONArray propertiesArray = new JSONArray()
              .put(new JSONObject().put("Name", "firstname").put("value", firstName))
              .put(new JSONObject().put("Name", "role").put("value", role))
              .put(new JSONObject().put("Name", "verification_status").put("value", emailVerificationStatus))
              .put(new JSONObject().put("Name", "stage").put("value", stage));

      log.debug("MAILJET - Property JSON array: {}", propertiesArray.toString());

      MailjetRequest request = new MailjetRequest(Contactdata.resource, mailjetId)
              .property(Contactdata.DATA, propertiesArray);

      MailjetResponse response = mailjetClient.put(request);

      log.debug("MAILJET - Mailjet PUT Contactdata response - Status: {}, Total: {}",
              response.getStatus(), response.getTotal());

      if (response.getTotal() != 1) {
        log.error("MAILJET - Failed to update user properties! Expected 1, got {} for Mailjet ID: {}",
                response.getTotal(), mailjetId);
        log.error("MAILJET - Response status: {}, Response data: {}",
                response.getStatus(), response.getData().toString());
        throw new MailjetException("Failed to update user! Response total: " + response.getTotal());
      }

      log.info("MAILJET - Successfully updated properties for Mailjet ID: {}", mailjetId);

    } catch (MailjetException e) {
      log.error("MAILJET - MailjetException updating properties for Mailjet ID: {}", mailjetId, e);
      throw e;
    } catch (JSONException e) {
      log.error("MAILJET - JSONException creating property data for Mailjet ID: {}", mailjetId, e);
      throw new MailjetException("Failed to create property JSON", e);
    } catch (Exception e) {
      log.error("MAILJET - Unexpected exception updating properties for Mailjet ID: {}", mailjetId, e);
      throw new MailjetException("Unexpected error updating properties: " + e.getMessage(), e);
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
    requireNonNull(mailjetId);

    log.info("MAILJET - Updating subscriptions for Mailjet ID: {}", mailjetId);
    log.debug("MAILJET - Subscriptions - news: {}, events: {}", newsEmails, eventsEmails);
    log.debug("MAILJET - Using list IDs - Legal: {}, News: {}, Events: {}", legalListId, newsListId, eventsListId);

    try {
      JSONArray subscriptionsArray = new JSONArray()
              .put(new JSONObject()
                      .put(ContactslistImportList.LISTID, legalListId)
                      .put(ContactslistImportList.ACTION, MailJetSubscriptionAction.FORCE_SUBSCRIBE.getValue()))
              .put(new JSONObject()
                      .put(ContactslistImportList.LISTID, newsListId)
                      .put(ContactslistImportList.ACTION, newsEmails.getValue()))
              .put(new JSONObject()
                      .put(ContactslistImportList.LISTID, eventsListId)
                      .put(ContactslistImportList.ACTION, eventsEmails.getValue()));

      log.debug("MAILJET - Subscription JSON array: {}", subscriptionsArray.toString());

      MailjetRequest request = new MailjetRequest(ContactManagecontactslists.resource, mailjetId)
              .property(ContactManagecontactslists.CONTACTSLISTS, subscriptionsArray);

      MailjetResponse response = mailjetClient.post(request);

      log.debug("MAILJET - Mailjet POST ContactManagecontactslists response - Status: {}, Total: {}",
              response.getStatus(), response.getTotal());

      if (response.getTotal() != 1) {
        log.error("MAILJET - Failed to update user subscriptions! Expected 1, got {} for Mailjet ID: {}",
                response.getTotal(), mailjetId);
        log.error("MAILJET - Response status: {}, Response data: {}",
                response.getStatus(), response.getData().toString());
        throw new MailjetException("Failed to update user subscriptions! Response total: " + response.getTotal());
      }

      log.info("MAILJET - Successfully updated subscriptions for Mailjet ID: {}", mailjetId);

    } catch (MailjetException e) {
      log.error("MAILJET - MailjetException updating subscriptions for Mailjet ID: {}", mailjetId, e);
      throw e;
    } catch (JSONException e) {
      log.error("MAILJET - JSONException creating subscription data for Mailjet ID: {}", mailjetId, e);
      throw new MailjetException("Failed to create subscription JSON", e);
    } catch (Exception e) {
      log.error("MAILJET - Unexpected exception updating subscriptions for Mailjet ID: {}", mailjetId, e);
      throw new MailjetException("Unexpected error updating subscriptions: " + e.getMessage(), e);
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