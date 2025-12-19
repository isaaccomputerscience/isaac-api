package uk.ac.cam.cl.dtg.segue.dao.users;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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
    // IMPORTANT: registered_contexts is JSONB[] (array of JSONB objects) in PostgreSQL
    // We use array_to_json() to convert it to proper JSON that Java can parse
    String query = "SELECT users.id, "
            + "       external_accounts.provider_user_identifier, "
            + "       users.email, "
            + "       users.role, "
            + "       users.given_name, "
            + "       users.deleted, "
            + "       users.email_verification_status, "
            + "       array_to_json(users.registered_contexts) AS registered_contexts, "  // Convert JSONB[] to JSON
            + "       news_prefs.preference_value AS news_emails, "
            + "       events_prefs.preference_value AS events_emails, "
            + "       external_accounts.provider_last_updated "
            + "FROM users "
            + "    LEFT OUTER JOIN user_preferences AS news_prefs "
            + "        ON users.id = news_prefs.user_id "
            + "        AND news_prefs.preference_type = 'EMAIL_PREFERENCE' "
            + "        AND news_prefs.preference_name = 'NEWS_AND_UPDATES' "
            + "    LEFT OUTER JOIN user_preferences AS events_prefs "
            + "        ON users.id = events_prefs.user_id "
            + "        AND events_prefs.preference_type = 'EMAIL_PREFERENCE' "
            + "        AND events_prefs.preference_name = 'EVENTS' "
            + "    LEFT OUTER JOIN external_accounts "
            + "        ON users.id = external_accounts.user_id "
            + "        AND external_accounts.provider_name = 'MailJet' "
            + "WHERE (users.last_updated >= external_accounts.provider_last_updated "
            + "       OR news_prefs.last_updated >= external_accounts.provider_last_updated "
            + "       OR events_prefs.last_updated >= external_accounts.provider_last_updated "
            + "       OR external_accounts.provider_last_updated IS NULL) "
            + "ORDER BY users.id";

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      log.debug("MAILJETT - Executing query to fetch recently changed user records");

      try (ResultSet results = pst.executeQuery()) {
        List<UserExternalAccountChanges> listOfResults = new ArrayList<>();

        while (results.next()) {
          try {
            UserExternalAccountChanges userChange = buildUserExternalAccountChanges(results);
            listOfResults.add(userChange);
          } catch (SQLException | JSONException e) {
            // Log but continue processing other users
            long userId = results.getLong("id");
            log.error("MAILJETT - Error building UserExternalAccountChanges for user ID: {}. Error: {}",
                    userId, e.getMessage(), e);
          }
        }

        log.debug("MAILJETT - Retrieved {} user records requiring synchronization", listOfResults.size());
        return listOfResults;
      }

    } catch (SQLException e) {
      log.error("MAILJETT - Database error while fetching recently changed records", e);
      throw new SegueDatabaseException("Failed to retrieve recently changed user records", e);
    }
  }

  @Override
  public void updateProviderLastUpdated(final Long userId) throws SegueDatabaseException {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }

    String query = "UPDATE external_accounts "
            + "SET provider_last_updated = ? "
            + "WHERE user_id = ? "
            + "AND provider_name = 'MailJet'";

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setTimestamp(1, Timestamp.from(Instant.now()));
      pst.setLong(2, userId);

      int rowsUpdated = pst.executeUpdate();

      if (rowsUpdated == 0) {
        log.warn("MAILJETT - No rows updated when setting provider_last_updated for user ID: {}. "
                + "User may not have an external_accounts record yet.", userId);
      } else {
        log.debug("MAILJETT - Updated provider_last_updated for user ID: {}", userId);
      }

    } catch (SQLException e) {
      log.error("MAILJETT - Database error updating provider_last_updated for user ID: {}", userId, e);
      throw new SegueDatabaseException("Failed to update provider_last_updated for user: " + userId, e);
    }
  }

  @Override
  public void updateExternalAccount(final Long userId, final String providerUserIdentifier)
          throws SegueDatabaseException {

    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }

    // Upsert the value in, using Postgres 9.5+ syntax 'ON CONFLICT DO UPDATE ...'
    String query = "INSERT INTO external_accounts (user_id, provider_name, provider_user_identifier) "
            + "VALUES (?, 'MailJet', ?) "
            + "ON CONFLICT (user_id, provider_name) "
            + "DO UPDATE SET provider_user_identifier = excluded.provider_user_identifier";

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(1, userId);
      pst.setString(2, providerUserIdentifier);

      int rowsAffected = pst.executeUpdate();

      if (rowsAffected > 0) {
        log.debug("MAILJETT - Upserted external_account for user ID: {} with Mailjet ID: {}",
                userId, providerUserIdentifier != null ? providerUserIdentifier : "[null]");
      } else {
        log.warn("MAILJETT - Upsert returned 0 rows for user ID: {}. This is unexpected.", userId);
      }

    } catch (SQLException e) {
      log.error("MAILJETT - Database error upserting external_account for user ID: {} with Mailjet ID: {}",
              userId, providerUserIdentifier, e);
      throw new SegueDatabaseException(
              "Failed to upsert external_account for user: " + userId, e);
    }
  }

  /**
   * Build UserExternalAccountChanges object from database result set.
   * Extracts stage information from registered_contexts JSONB[] field.
   * Parses boolean preference values with proper null handling.
   */
  private UserExternalAccountChanges buildUserExternalAccountChanges(final ResultSet results)
          throws SQLException {

    Long userId = results.getLong("id");

    // Parse registered_contexts (JSONB[] -> String -> stage)
    String registeredContextsJson = results.getString("registered_contexts");
    String stage = extractStageFromRegisteredContexts(userId, registeredContextsJson);

    // Parse boolean preferences with null handling
    Boolean newsEmails = parseBooleanPreference(userId, "NEWS_AND_UPDATES", results, "news_emails");
    Boolean eventsEmails = parseBooleanPreference(userId, "EVENTS", results, "events_emails");

    return new UserExternalAccountChanges(
            userId,
            results.getString("provider_user_identifier"),
            results.getString("email"),
            Role.valueOf(results.getString("role")),
            results.getString("given_name"),
            results.getBoolean("deleted"),
            EmailVerificationStatus.valueOf(results.getString("email_verification_status")),
            newsEmails,
            eventsEmails,
            stage
    );
  }

  /**
   * Parse boolean preference value from ResultSet with proper null handling.
   * PostgreSQL boolean columns can be NULL, which JDBC returns as false by default.
   * We need to check wasNull() to distinguish between false and NULL.
   *
   * @param userId User ID for logging
   * @param preferenceName Name of preference for logging
   * @param results ResultSet containing the data
   * @param columnName Column name in ResultSet
   * @return Boolean value (true/false/null)
   */
  private Boolean parseBooleanPreference(Long userId, String preferenceName,
                                         ResultSet results, String columnName) throws SQLException {
    boolean value = results.getBoolean(columnName);
    boolean wasNull = results.wasNull();

    if (wasNull) {
      // User has no preference set - treat as null (not subscribed)
      log.debug("MAILJETT - User ID {} has NULL preference for {}. Treating as not subscribed.",
              userId, preferenceName);
      return null;
    }

    log.debug("MAILJETT - User ID {} has preference {} = {}", userId, preferenceName, value);
    return value;
  }

  /**
   * Extract stage information from registered_contexts JSONB[] field.
   *
   * PostgreSQL JSONB[] is converted to JSON using array_to_json() in the query.
   * This gives us clean JSON like: [{"stage": "gcse", "examBoard": "aqa"}]
   *
   * @param userId User ID for logging
   * @param registeredContextsJson JSONB[] converted to JSON via array_to_json()
   * @return stage string: "GCSE", "A Level", "GCSE and A Level", or "unknown"
   */
  private String extractStageFromRegisteredContexts(Long userId, String registeredContextsJson) {
    if (registeredContextsJson == null || registeredContextsJson.trim().isEmpty()) {
      log.debug("MAILJETT - User ID {} has NULL/empty registered_contexts. Stage: unknown", userId);
      return "unknown";
    }

    String trimmed = registeredContextsJson.trim();

    // Check for empty JSON array
    if ("[]".equals(trimmed) || "null".equals(trimmed)) {
      log.debug("MAILJETT - User ID {} has empty/null registered_contexts. Stage: unknown", userId);
      return "unknown";
    }

    try {
      // Parse as JSONArray (array_to_json returns proper JSON array)
      JSONArray array = new JSONArray(trimmed);

      if (array.length() == 0) {
        log.debug("MAILJETT - User ID {} has empty JSON array in registered_contexts. Stage: unknown", userId);
        return "unknown";
      }

      // Search through array for 'stage' key
      for (int i = 0; i < array.length(); i++) {
        Object item = array.get(i);
        if (item instanceof JSONObject) {
          JSONObject obj = (JSONObject) item;
          if (obj.has("stage")) {
            String stage = obj.getString("stage");
            String normalized = normalizeStage(stage);
            log.debug("MAILJETT - User ID {} has stage '{}' in registered_contexts[{}]. Normalized: {}",
                    userId, stage, i, normalized);
            return normalized;
          }
        }
      }

      // No 'stage' key found, use fallback pattern matching
      String fallbackStage = fallbackStageDetection(userId, trimmed);
      if (!"unknown".equals(fallbackStage)) {
        log.debug("MAILJETT - User ID {} stage detected via fallback pattern matching: {}", userId, fallbackStage);
      } else {
        log.warn("MAILJETT - User ID {} has registered_contexts but no 'stage' key found: {}. Stage: unknown",
                userId, truncateForLog(trimmed));
      }
      return fallbackStage;

    } catch (JSONException e) {
      log.warn("MAILJETT - User ID {} has invalid JSON in registered_contexts: '{}'. Error: {}. Stage: unknown",
              userId, truncateForLog(registeredContextsJson), e.getMessage());
      return "unknown";
    }
  }

  /**
   * Fallback stage detection by pattern matching in the JSON string.
   * Used when no explicit 'stage' key is found.
   */
  private String fallbackStageDetection(Long userId, String jsonString) {
    String lower = jsonString.toLowerCase();
    boolean hasGcse = lower.contains("gcse");
    boolean hasALevel = lower.contains("a_level") || lower.contains("alevel") || lower.contains("a level");

    if (hasGcse && hasALevel) {
      return "GCSE and A Level";
    } else if (hasGcse) {
      return "GCSE";
    } else if (hasALevel) {
      return "A Level";
    }

    return "unknown";
  }

  /**
   * Normalize stage values to consistent format for Mailjet.
   */
  private String normalizeStage(String stage) {
    if (stage == null || stage.trim().isEmpty()) {
      return "unknown";
    }

    String normalized = stage.trim().toLowerCase();

    switch (normalized) {
      case "gcse":
        return "GCSE";
      case "a_level":
      case "a level":
      case "alevel":
      case "a-level":
        return "A Level";
      case "gcse_and_a_level":
      case "gcse and a level":
      case "both":
      case "gcse,a_level":
      case "gcse, a level":
        return "GCSE and A Level";
      default:
        // Warn about unexpected stage values
        log.warn("MAILJETT - Unexpected stage value '{}' encountered. Returning 'unknown'. "
                + "Expected values: gcse, a_level, gcse_and_a_level, both", stage);
        return "unknown";
    }
  }

  /**
   * Truncate long strings for logging to avoid cluttering logs.
   */
  private String truncateForLog(String str) {
    if (str == null) {
      return "null";
    }
    if (str.length() <= 100) {
      return str;
    }
    return str.substring(0, 97) + "...";
  }
}
