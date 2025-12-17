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
    String query = "SELECT users.id, provider_user_identifier, email, role, given_name, deleted, email_verification_status, "
            + "       registered_contexts, "
            + "       news_prefs.preference_value AS news_emails, "
            + "       events_prefs.preference_value AS events_emails "
            + "FROM users "
            + "    LEFT OUTER JOIN user_preferences AS news_prefs ON users.id = news_prefs.user_id "
            + "        AND news_prefs.preference_type='EMAIL_PREFERENCE' "
            + "        AND news_prefs.preference_name='NEWS_AND_UPDATES' "
            + "    LEFT OUTER JOIN user_preferences AS events_prefs ON users.id = events_prefs.user_id "
            + "        AND events_prefs.preference_type='EMAIL_PREFERENCE' "
            + "        AND events_prefs.preference_name='EVENTS' "
            + "    LEFT OUTER JOIN external_accounts ON users.id=external_accounts.user_id "
            + "        AND provider_name='MailJet' "
            + "WHERE (users.last_updated >= provider_last_updated "
            + "       OR news_prefs.last_updated >= provider_last_updated "
            + "       OR events_prefs.last_updated >= provider_last_updated "
            + "       OR provider_last_updated IS NULL) "
            + "ORDER BY users.id";

    log.info("MAILJET - Fetching recently changed records for sync");

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query);
         ResultSet results = pst.executeQuery()
    ) {
      List<UserExternalAccountChanges> listOfResults = Lists.newArrayList();

      while (results.next()) {
        listOfResults.add(buildUserExternalAccountChanges(results));
      }

      log.info("MAILJET - Retrieved {} users requiring synchronisation", listOfResults.size());
      return listOfResults;

    } catch (SQLException e) {
      log.error("MAILJET - Database error fetching recently changed records", e);
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

  /**
   * Builds a UserExternalAccountChanges object from a ResultSet.
   * Properly parses the registered_contexts array which contains JSON strings like:
   * {"{\"stage\": \"gcse\", \"examBoard\": \"all\"}","{\"stage\": \"a_level\", \"examBoard\": \"all\"}"}
   *
   * @param results the ResultSet containing user data
   * @return UserExternalAccountChanges object
   * @throws SQLException if database access fails
   */
  private UserExternalAccountChanges buildUserExternalAccountChanges(final ResultSet results) throws SQLException {
    Long userId = results.getLong("id");

    try {
      // Extract basic fields
      String providerUserId = results.getString("provider_user_identifier");
      String email = results.getString("email");
      String roleStr = results.getString("role");
      Role role = Role.valueOf(roleStr);
      String givenName = results.getString("given_name");
      boolean deleted = results.getBoolean("deleted");
      String emailVerificationStatusStr = results.getString("email_verification_status");
      EmailVerificationStatus emailVerificationStatus = EmailVerificationStatus.valueOf(emailVerificationStatusStr);

      // Handle nullable booleans from LEFT OUTER JOIN
      Boolean newsEmails = (Boolean) results.getObject("news_emails");
      Boolean eventsEmails = (Boolean) results.getObject("events_emails");

      // Parse registered_contexts array to extract stage
      String stage = parseStageFromRegisteredContexts(results.getString("registered_contexts"), userId);

      return new UserExternalAccountChanges(
              userId,
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

    } catch (Exception e) {
      log.error("MAILJET - Failed to build UserExternalAccountChanges for user {}: {}", userId, e.getMessage());
      throw new SQLException("Failed to parse user data for user " + userId, e);
    }
  }

  /**
   * Parses the registered_contexts PostgreSQL array to extract stage information.
   * The array contains JSON strings like: {"{\"stage\": \"gcse\"}","{\"stage\": \"a_level\"}"}
   * or is empty: {}
   *
   * @param registeredContextsStr the raw string from the database
   * @param userId the user ID for logging purposes
   * @return a stage string: "gcse", "a_level", "gcse_and_a_level", or "not_specified"
   */
  private String parseStageFromRegisteredContexts(String registeredContextsStr, Long userId) {
    if (registeredContextsStr == null || registeredContextsStr.trim().isEmpty() || registeredContextsStr.equals("{}")) {
      return "not_specified";
    }

    try {
      // PostgreSQL array format: {"element1","element2"}
      // Remove the outer braces and split by comma (handling quoted strings)
      String cleaned = registeredContextsStr.trim();
      if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
        cleaned = cleaned.substring(1, cleaned.length() - 1);
      }

      boolean hasGcse = false;
      boolean hasALevel = false;

      // Check if the string contains stage information
      // The array elements are JSON strings like "{\"stage\": \"gcse\", \"examBoard\": \"all\"}"
      if (cleaned.toLowerCase().contains("\"stage\"") || cleaned.toLowerCase().contains("stage")) {
        // Simple pattern matching - more reliable than parsing complex nested structures
        String lowerCleaned = cleaned.toLowerCase();
        hasGcse = lowerCleaned.contains("\"stage\": \"gcse\"") || lowerCleaned.contains("stage\": \"gcse");
        hasALevel = lowerCleaned.contains("\"stage\": \"a_level\"") || lowerCleaned.contains("stage\": \"a_level");
      }

      if (hasGcse && hasALevel) {
        return "gcse_and_a_level";
      } else if (hasGcse) {
        return "gcse";
      } else if (hasALevel) {
        return "a_level";
      } else {
        return "not_specified";
      }

    } catch (Exception e) {
      log.warn("MAILJET - Failed to parse registered_contexts for user {}: {}. Raw value: {}",
              userId, e.getMessage(), registeredContextsStr);
      return "not_specified";
    }
  }
}
