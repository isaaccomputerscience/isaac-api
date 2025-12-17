package uk.ac.cam.cl.dtg.segue.dao.users;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * This class is responsible for managing and persisting user data.
 */
public class PgExternalAccountPersistenceManager implements IExternalAccountDataManager {
  private static final Logger log = LoggerFactory.getLogger(PgExternalAccountPersistenceManager.class);

  private final PostgresSqlDb database;

  /**
   * Creates a new user data manager object.
   *
   * @param database - the database reference used for persistence.
   */
  @Inject
  public PgExternalAccountPersistenceManager(final PostgresSqlDb database) {
    this.database = database;
  }

  @Override
  public List<UserExternalAccountChanges> getRecentlyChangedRecords() throws SegueDatabaseException {
    String query = "SELECT id, provider_user_identifier, email, role, given_name, deleted, email_verification_status, "
            + "       registered_contexts, "
            + "       news_prefs.preference_value AS news_emails, events_prefs.preference_value AS events_emails "
            + "FROM users "
            + "    LEFT OUTER JOIN user_preferences AS news_prefs ON users.id = news_prefs.user_id "
            + "AND news_prefs.preference_type='EMAIL_PREFERENCE' "
            + "AND news_prefs.preference_name='NEWS_AND_UPDATES' "
            + "    LEFT OUTER JOIN user_preferences AS events_prefs ON users.id = events_prefs.user_id "
            + "AND events_prefs.preference_type='EMAIL_PREFERENCE' "
            + "AND events_prefs.preference_name='EVENTS' "
            + "    LEFT OUTER JOIN external_accounts ON users.id=external_accounts.user_id AND provider_name='MailJet' "
            + "WHERE (users.last_updated >= provider_last_updated OR news_prefs.last_updated >= provider_last_updated "
            + "           OR events_prefs.last_updated >= provider_last_updated OR provider_last_updated IS NULL)";

      log.info("MMAILJETT - QUERY :{}", query);
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query);
         ResultSet results = pst.executeQuery()
    ) {
      log.info("MMAILJETT - SIZE: {}", results.getFetchSize());
      List<UserExternalAccountChanges> listOfResults = Lists.newArrayList();
      log.info("MMAILJETT - LIST SIZE BEFORE: {}", listOfResults.size());
      while (results.next()) {
        listOfResults.add(buildUserExternalAccountChanges(results));
      }
      log.info("MMAILJETT - LIST SIZE AFTER: {}", listOfResults.size());
      return listOfResults;

    } catch (SQLException e) {
      log.info("MMAILJETT - QUERY :{}", query);
      log.error("MMAILJETT - ERROR : {}", e.toString());
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public void updateProviderLastUpdated(final Long userId) throws SegueDatabaseException {
    String query = "UPDATE external_accounts SET provider_last_updated=? WHERE user_id=? AND provider_name='MailJet';";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setTimestamp(1, Timestamp.from(Instant.now()));
      pst.setLong(2, userId);

      pst.executeUpdate();
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception on update ", e);
    }
  }

  @Override
  public void updateExternalAccount(final Long userId, final String providerUserIdentifier)
      throws SegueDatabaseException {
    // Upsert the value in, using Postgres 9.5 syntax 'ON CONFLICT DO UPDATE ...'
    String query =
        "INSERT INTO external_accounts(user_id, provider_name, provider_user_identifier) VALUES (?, 'MailJet', ?)"
            + " ON CONFLICT (user_id, provider_name) DO UPDATE SET"
            + " provider_user_identifier=excluded.provider_user_identifier";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(1, userId);
      pst.setString(2, providerUserIdentifier);

      pst.executeUpdate();
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception on upsert ", e);
    }
  }

  private UserExternalAccountChanges buildUserExternalAccountChanges(final ResultSet results) throws SQLException {
    log.info("MMAILJETT - buildUserExternalAccountChanges - START");

    String registeredContextsJson = results.getString("registered_contexts");
    log.info("MMAILJETT - registeredContextsJson: {}", registeredContextsJson);

    // Parse the JSON string if it's not null
    JSONObject registeredContexts = null;
    if (registeredContextsJson != null && !registeredContextsJson.isEmpty()) {
      try {
        JSONObject outer = new JSONObject(registeredContextsJson);
        log.info("MMAILJETT - Parsed outer JSON object, length: {}", outer.length());

        // Check if it's double-encoded (has a single key that's a JSON string)
        if (outer.length() == 1) {
          String[] keys = JSONObject.getNames(outer);
          log.info("MMAILJETT - Single key found: {}", keys != null && keys.length > 0 ? keys[0] : "null");

          if (keys != null && keys.length == 1) {
            String firstKey = keys[0];
            // If the key itself looks like JSON (starts with {), it's double-encoded
            if (firstKey.startsWith("{")) {
              log.info("MMAILJETT - Detected double-encoded JSON, parsing inner object");
              registeredContexts = new JSONObject(firstKey);
              log.info("MMAILJETT - Parsed inner JSON object: {}", registeredContexts.toString());
            } else {
              log.info("MMAILJETT - Using outer object as-is");
              registeredContexts = outer;
            }
          }
        } else if (outer.length() > 1) {
          log.info("MMAILJETT - Multiple keys found, using outer object as-is");
          // Normal JSON object
          registeredContexts = outer;
        } else {
          log.info("MMAILJETT - Empty JSON object, registeredContexts will be null");
        }
        // If length is 0 (empty object {}), registeredContexts stays null

      } catch (JSONException e) {
        log.warn("MMAILJETT - Failed to parse registered_contexts JSON for user {}: {}",
                results.getLong("id"), e.getMessage());
      }
    } else {
      log.info("MMAILJETT - registeredContextsJson is null or empty");
    }

    // Extract stage from the JSON object, or use a default value
    String stage = (registeredContexts != null && registeredContexts.has("stage"))
            ? registeredContexts.getString("stage")
            : "unknown";
    log.info("MMAILJETT - Extracted stage: {}", stage);

    // Handle nullable booleans from LEFT OUTER JOIN
    Boolean newsEmails = (Boolean) results.getObject("news_emails");
    log.info("MMAILJETT - newsEmails: {}", newsEmails);

    Boolean eventsEmails = (Boolean) results.getObject("events_emails");
    log.info("MMAILJETT - eventsEmails: {}", eventsEmails);

    Long id = results.getLong("id");
    log.info("MMAILJETT - id: {}", id);

    String providerUserId = results.getString("provider_user_identifier");
    log.info("MMAILJETT - provider_user_identifier: {}", providerUserId);

    String email = results.getString("email");
    log.info("MMAILJETT - email: {}", email);

    String roleStr = results.getString("role");
    log.info("MMAILJETT - role string: {}", roleStr);
    Role role = Role.valueOf(roleStr);
    log.info("MMAILJETT - role enum: {}", role);

    String givenName = results.getString("given_name");
    log.info("MMAILJETT - given_name: {}", givenName);

    boolean deleted = results.getBoolean("deleted");
    log.info("MMAILJETT - deleted: {}", deleted);

    String emailVerificationStatusStr = results.getString("email_verification_status");
    log.info("MMAILJETT - email_verification_status string: {}", emailVerificationStatusStr);
    EmailVerificationStatus emailVerificationStatus = EmailVerificationStatus.valueOf(emailVerificationStatusStr);
    log.info("MMAILJETT - email_verification_status enum: {}", emailVerificationStatus);

    log.info("MMAILJETT - buildUserExternalAccountChanges - Creating UserExternalAccountChanges object");

    UserExternalAccountChanges result = new UserExternalAccountChanges(
            id,
            providerUserId,
            email,
            role,
            givenName,
            deleted,
            emailVerificationStatus,
            newsEmails,
            eventsEmails,
            stage
    );

    log.info("MMAILJETT - buildUserExternalAccountChanges - END, returning object for user {}", id);

    return result;
  }
}
