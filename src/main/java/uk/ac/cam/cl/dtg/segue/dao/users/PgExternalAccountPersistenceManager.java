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

  private static final String STAGE_UNKNOWN = "unknown";
  private static final String MAILJET = "MAILJET - ";

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

    return executeQueryAndBuildUserRecords(query);
  }

  /**
   * Execute query and build user records list.
   * Extracted to reduce nesting complexity.
   */
  private List<UserExternalAccountChanges> executeQueryAndBuildUserRecords(String query)
      throws SegueDatabaseException {
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      log.debug("{} Executing query to fetch recently changed user records", MAILJET);

      try (ResultSet results = pst.executeQuery()) {
        List<UserExternalAccountChanges> listOfResults = new ArrayList<>();

        while (results.next()) {
          extracted(results, listOfResults);
        }

        log.debug("{} Retrieved {} user records requiring synchronization", MAILJET, listOfResults.size());
        return listOfResults;
      }

    } catch (SQLException e) {
      String errorMsg = "Database error while fetching recently changed records";
      throw new SegueDatabaseException(errorMsg + ": " + e.getMessage(), e);
    }
  }

  private void extracted(ResultSet results, List<UserExternalAccountChanges> listOfResults) throws SQLException {
    try {
      UserExternalAccountChanges userChange = buildUserExternalAccountChanges(results);
      listOfResults.add(userChange);
    } catch (SQLException | JSONException e) {
      long userId = results.getLong("id");
      log.error("{} Error building UserExternalAccountChanges for user ID: {}. "
              + "Error type: {}, Message: {}. Skipping this user and continuing with next.",
          MAILJET, userId, e.getClass().getSimpleName(), e.getMessage(), e);
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
        log.warn("{} No rows updated when setting provider_last_updated for user ID: {}. "
            + "User may not have an external_accounts record yet.", MAILJET, userId);
      } else {
        log.debug("{} Updated provider_last_updated for user ID: {}", MAILJET, userId);
      }

    } catch (SQLException e) {
      String errorMsg = String.format("Database error updating provider_last_updated for user ID: %d", userId);
      throw new SegueDatabaseException(errorMsg + ": " + e.getMessage(), e);
    }
  }

  @Override
  public void updateExternalAccount(final Long userId, final String providerUserIdentifier)
      throws SegueDatabaseException {

    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }

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
        log.debug("{} Upserted external_account for user ID: {} with Mailjet ID: {}",
            MAILJET, userId, providerUserIdentifier != null ? providerUserIdentifier : "[null]");
      } else {
        log.warn("{} Upsert returned 0 rows for user ID: {}. This is unexpected.", MAILJET, userId);
      }

    } catch (SQLException e) {
      String errorMsg = String.format("Database error upserting external_account for user ID: %d with Mailjet ID: %s",
          userId, providerUserIdentifier);
      throw new SegueDatabaseException(errorMsg + ": " + e.getMessage(), e);
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

    String registeredContextsJson = results.getString("registered_contexts");
    String stage = extractStageFromRegisteredContexts(userId, registeredContextsJson);

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
   * @param userId         User ID for logging
   * @param preferenceName Name of preference for logging
   * @param results        ResultSet containing the data
   * @param columnName     Column name in ResultSet
   * @return Boolean value (true/false/null)
   */
  @javax.annotation.CheckForNull
  private Boolean parseBooleanPreference(Long userId, String preferenceName,
                                         ResultSet results, String columnName) throws SQLException {
    boolean value = results.getBoolean(columnName);
    boolean wasNull = results.wasNull();

    if (wasNull) {
      if (log.isDebugEnabled()) {
        log.debug("{} User ID {} has NULL preference for {}. Treating as not subscribed.",
            MAILJET, userId, preferenceName);
      }
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("{} User ID {} has preference {} = {}", MAILJET, userId, preferenceName, value);
    }
    return value;
  }

  /**
   * Extract stage information from registered_contexts JSONB[] field.
   *
   * <p>
   * PostgreSQL JSONB[] is converted to JSON using array_to_json() in the query.
   * This gives us clean JSON like: [{"stage": "gcse", "examBoard": "aqa"}]
   *
   * @param userId                 User ID for logging
   * @param registeredContextsJson JSONB[] converted to JSON via array_to_json()
   * @return stage string: "GCSE", "A Level", "GCSE and A Level", or "unknown"
   */
  private String extractStageFromRegisteredContexts(Long userId, String registeredContextsJson) {
    if (isNullOrEmpty(registeredContextsJson)) {
      logDebugStage(userId, "NULL/empty registered_contexts");
      return STAGE_UNKNOWN;
    }

    String trimmed = registeredContextsJson.trim();

    if (isEmptyJsonArray(trimmed)) {
      logDebugStage(userId, "empty/null registered_contexts");
      return STAGE_UNKNOWN;
    }

    return parseJsonArrayForStage(userId, trimmed, registeredContextsJson);
  }

  /**
   * Check if string is null or empty.
   */
  private boolean isNullOrEmpty(String str) {
    return str == null || str.trim().isEmpty();
  }

  /**
   * Check if trimmed string represents an empty JSON array.
   */
  private boolean isEmptyJsonArray(String trimmed) {
    return "[]".equals(trimmed) || "null".equals(trimmed);
  }

  /**
   * Log debug message for stage determination.
   */
  private void logDebugStage(Long userId, String reason) {
    if (log.isDebugEnabled()) {
      log.debug("{} User ID {} has {}. Stage: {}", MAILJET, userId, reason, STAGE_UNKNOWN);
    }
  }

  /**
   * Parse JSON array to extract stage information.
   * Reduces cognitive complexity by extracting try-catch logic.
   */
  private String parseJsonArrayForStage(Long userId, String trimmed, String originalJson) {
    try {
      JSONArray array = new JSONArray(trimmed);

      if (array.isEmpty()) {
        logDebugStage(userId, "empty JSON array in registered_contexts");
        return STAGE_UNKNOWN;
      }

      String stageFromArray = extractStageFromArray(userId, array);
      if (stageFromArray != null) {
        return stageFromArray;
      }

      return handleMissingStage(userId, trimmed);

    } catch (JSONException e) {
      log.warn("{} User ID {} has invalid JSON in registered_contexts: '{}'. "
              + "Error type: {}, Message: {}. Stage: {}",
          MAILJET, userId, truncateForLog(originalJson), e.getClass().getSimpleName(),
          e.getMessage(), STAGE_UNKNOWN);
      return STAGE_UNKNOWN;
    }
  }

  /**
   * Extract stage from JSON array items.
   */
  private String extractStageFromArray(Long userId, JSONArray array) {
    for (int i = 0; i < array.length(); i++) {
      Object item = array.get(i);
      if (item instanceof JSONObject obj && obj.has("stage")) {
        String stage = obj.getString("stage");
        String normalized = normalizeStage(stage);
        if (log.isDebugEnabled()) {
          log.debug("{} User ID {} has stage '{}' in registered_contexts[{}]. Normalized: {}",
              MAILJET, userId, stage, i, normalized);
        }
        return normalized;
      }
    }
    return null;
  }

  /**
   * Handle case where no stage key was found in JSON.
   */
  private String handleMissingStage(Long userId, String trimmed) {
    String fallbackStage = fallbackStageDetection(trimmed);
    if (!STAGE_UNKNOWN.equals(fallbackStage)) {
      if (log.isDebugEnabled()) {
        log.debug("{} User ID {} stage detected via fallback pattern matching: {}",
            MAILJET, userId, fallbackStage);
      }
    } else {
      log.warn("{} User ID {} has registered_contexts but no 'stage' key found: {}. Stage: {}",
          MAILJET, userId, trimmed, STAGE_UNKNOWN);
    }
    return fallbackStage;
  }

  /**
   * Fallback stage detection by pattern matching in the JSON string.
   * Used when no explicit 'stage' key is found.
   */
  private String fallbackStageDetection(String jsonString) {
    String lower = jsonString.toLowerCase();
    boolean hasGcse = lower.contains("gcse");
    boolean hasLevel = lower.contains("a_level") || lower.contains("alevel") || lower.contains("a level");

    if (hasGcse && hasLevel) {
      return "GCSE and A Level";
    } else if (hasGcse) {
      return "GCSE";
    } else if (hasLevel) {
      return "A Level";
    }

    return STAGE_UNKNOWN;
  }

  /**
   * Normalize stage values to consistent format for Mailjet.
   */
  private String normalizeStage(String stage) {
    if (stage == null || stage.trim().isEmpty()) {
      return STAGE_UNKNOWN;
    }

    String normalized = stage.trim().toLowerCase();

    return switch (normalized) {
      case "gcse" -> "GCSE";
      case "a_level", "a level", "alevel", "a-level" -> "A Level";
      case "gcse_and_a_level", "gcse and a level", "both", "gcse,a_level", "gcse, a level" -> "GCSE and A Level";
      case "all" -> "ALL";
      default -> {
        log.warn("{} Unexpected stage value '{}' encountered. Returning '{}'. "
            + "Expected values: gcse, a_level, gcse_and_a_level, both", MAILJET, stage, STAGE_UNKNOWN);
        yield STAGE_UNKNOWN;
      }
    };
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
