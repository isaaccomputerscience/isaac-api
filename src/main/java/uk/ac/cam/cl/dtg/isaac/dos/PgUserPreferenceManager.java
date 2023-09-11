/*
 * Copyright 2017 James Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dos;

import static java.lang.Math.min;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.SEARCH_MAX_WINDOW_SIZE;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * A postgres specific User Preference Manager.
 */
public class PgUserPreferenceManager extends AbstractUserPreferenceManager {

  private final PostgresSqlDb database;
  private static final Logger log = LoggerFactory.getLogger(PgUserPreferenceManager.class);

  /**
   * Create a PgUserPreferenceManager.
   *
   * @param database - a pre-configured postgres database object
   */
  @Inject
  public PgUserPreferenceManager(final PostgresSqlDb database) {
    this.database = database;
  }

  private UserPreference userPreferenceFromResultSet(final ResultSet results) throws SQLException {
    return new UserPreference(results.getLong("user_id"), results.getString("preference_type"),
        results.getString("preference_name"), results.getBoolean("preference_value"));
  }

  @Override
  public UserPreference getUserPreference(final String preferenceType, final String preferenceName, final long userId)
      throws SegueDatabaseException {
    Validate.notBlank(preferenceType);
    Validate.notBlank(preferenceName);

    String query = "SELECT * FROM user_preferences WHERE user_id=? AND preference_type=? AND preference_name=?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_PREFERENCE_USER_ID, userId);
      pst.setString(FIELD_GET_PREFERENCE_PREFERENCE_TYPE, preferenceType);
      pst.setString(FIELD_GET_PREFERENCE_PREFERENCE_NAME, preferenceName);
      pst.setMaxRows(1); // There is a primary key to ensure uniqueness!

      try (ResultSet results = pst.executeQuery()) {
        if (results.next()) {
          return userPreferenceFromResultSet(results);
        }
        // We must not have found anything:
        return null;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public Map<Long, UserPreference> getUsersPreference(final String preferenceType, final String preferenceName,
                                                      final List<RegisteredUserDTO> users)
      throws SegueDatabaseException {
    Validate.notBlank(preferenceType);
    Validate.notBlank(preferenceName);

    Map<Long, UserPreference> usersPreferenceMap = Maps.newHashMap();

    int pageSize = SEARCH_MAX_WINDOW_SIZE;
    int fromIndex = 0;
    int toIndex = min(pageSize, users.size());

    while (fromIndex < toIndex) {

      List<RegisteredUserDTO> pagedUsers = users.subList(fromIndex, toIndex);
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT * FROM user_preferences WHERE user_id IN (");
      for (int i = 0; i < pagedUsers.size(); i++) {
        sb.append("?").append(i < pagedUsers.size() - 1 ? ", " : "");
      }
      sb.append(") AND preference_type=? AND preference_name=? ORDER BY user_id ASC;");

      try (Connection conn = database.getDatabaseConnection();
           PreparedStatement pst = conn.prepareStatement(sb.toString())
      ) {
        for (int i = 1; i <= pagedUsers.size(); i++) {
          pst.setLong(i, pagedUsers.get(i - 1).getId());
        }
        pst.setString(pagedUsers.size() + 1, preferenceType);
        pst.setString(pagedUsers.size() + 2, preferenceName);

        try (ResultSet results = pst.executeQuery()) {

          while (results.next()) {
            Long userId = results.getLong("user_id");
            UserPreference pref = userPreferenceFromResultSet(results);
            usersPreferenceMap.put(userId, pref);
          }
        }

        fromIndex = toIndex;
        toIndex = min(toIndex + pageSize, users.size());

      } catch (SQLException e) {
        throw new SegueDatabaseException("Postgres exception", e);
      }
    }
    return usersPreferenceMap;
  }

  @Override
  public List<UserPreference> getUserPreferences(final String preferenceType, final long userId)
      throws SegueDatabaseException {
    Validate.notBlank(preferenceType);

    String query = "SELECT * FROM user_preferences WHERE user_id=? AND preference_type=?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_PREFERENCES_USER_ID, userId);
      pst.setString(FIELD_GET_PREFERENCES_PREFERENCE_TYPE, preferenceType);

      try (ResultSet results = pst.executeQuery()) {
        List<UserPreference> userPreferences = Lists.newArrayList();

        while (results.next()) {
          UserPreference pref = userPreferenceFromResultSet(results);
          userPreferences.add(pref);
        }

        return userPreferences;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public List<UserPreference> getAllUserPreferences(final long userId) throws SegueDatabaseException {

    String query = "SELECT * FROM user_preferences WHERE user_id=?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_ALL_PREFERENCES_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {

        List<UserPreference> userPreferences = Lists.newArrayList();

        while (results.next()) {
          UserPreference pref = userPreferenceFromResultSet(results);
          userPreferences.add(pref);
        }

        return userPreferences;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public Map<Long, List<UserPreference>> getUserPreferences(final String preferenceType,
                                                            final List<RegisteredUserDTO> users)
      throws SegueDatabaseException {
    Validate.notBlank(preferenceType);

    Map<Long, List<UserPreference>> usersPreferencesMap = Maps.newHashMap();

    int pageSize = SEARCH_MAX_WINDOW_SIZE;
    int fromIndex = 0;
    int toIndex = min(pageSize, users.size());

    while (fromIndex < toIndex) {

      List<RegisteredUserDTO> pagedUsers = users.subList(fromIndex, toIndex);
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT * FROM user_preferences WHERE user_id IN (");

      for (int i = 0; i < pagedUsers.size(); i++) {
        sb.append("?").append(i < pagedUsers.size() - 1 ? ", " : "");
      }
      sb.append(") AND preference_type=? ORDER BY user_id ASC, preference_name ASC;");

      try (Connection conn = database.getDatabaseConnection();
           PreparedStatement pst = conn.prepareStatement(sb.toString())
      ) {
        for (int i = 1; i <= pagedUsers.size(); i++) {
          pst.setLong(i, pagedUsers.get(i - 1).getId());
        }
        pst.setString(pagedUsers.size() + 1, preferenceType);

        try (ResultSet results = pst.executeQuery()) {

          while (results.next()) {
            Long userId = results.getLong("user_id");
            UserPreference pref = userPreferenceFromResultSet(results);
            List<UserPreference> values;
            if (usersPreferencesMap.containsKey(userId) && usersPreferencesMap.get(userId) != null) {
              values = usersPreferencesMap.get(userId);
            } else {
              values = Lists.newArrayList();
              usersPreferencesMap.put(userId, values);
            }
            values.add(pref);
          }
        }

        fromIndex = toIndex;
        toIndex = min(toIndex + pageSize, users.size());

      } catch (SQLException e) {
        throw new SegueDatabaseException("Postgres exception", e);
      }
    }
    return usersPreferencesMap;
  }

  @Override
  public void saveUserPreferences(final List<UserPreference> userPreferences) throws SegueDatabaseException {
    // Upsert the value in, using Postgres 9.5 syntax 'ON CONFLICT DO UPDATE ...'
    // Only update a conflicting row if value has changed, to ensure the last_updated date remains accurate:
    String query =
        "INSERT INTO user_preferences(user_id, preference_type, preference_name, preference_value, last_updated) "
            + " VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)"
            + " ON CONFLICT (user_id, preference_type, preference_name) DO UPDATE"
            + " SET preference_value=excluded.preference_value, last_updated=excluded.last_updated"
            + " WHERE user_preferences.preference_value!=excluded.preference_value;";
    try (Connection conn = database.getDatabaseConnection()) {
      conn.setAutoCommit(false);
      for (UserPreference preference : userPreferences) {
        try (PreparedStatement pst = conn.prepareStatement(query)) {
          pst.setLong(FIELD_SAVE_PREFERENCES_USER_ID, preference.getUserId());
          pst.setString(FIELD_SAVE_PREFERENCES_PREFERENCE_TYPE, preference.getPreferenceType());
          pst.setString(FIELD_SAVE_PREFERENCES_PREFERENCE_NAME, preference.getPreferenceName());
          pst.setBoolean(FIELD_SAVE_PREFERENCES_PREFERENCE_VALUE, preference.getPreferenceValue());

          pst.executeUpdate();
        }
      }
      conn.commit();
      conn.setAutoCommit(true);
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception on upsert ", e);
    }
  }

  // Field Constants
  // getUserPreference
  private static final int FIELD_GET_PREFERENCE_USER_ID = 1;
  private static final int FIELD_GET_PREFERENCE_PREFERENCE_TYPE = 2;
  private static final int FIELD_GET_PREFERENCE_PREFERENCE_NAME = 3;

  // getUserPreferences
  private static final int FIELD_GET_PREFERENCES_USER_ID = 1;
  private static final int FIELD_GET_PREFERENCES_PREFERENCE_TYPE = 2;

  // getAllUserPreferences
  private static final int FIELD_GET_ALL_PREFERENCES_USER_ID = 1;

  // saveUserPreferences
  private static final int FIELD_SAVE_PREFERENCES_USER_ID = 1;
  private static final int FIELD_SAVE_PREFERENCES_PREFERENCE_TYPE = 2;
  private static final int FIELD_SAVE_PREFERENCES_PREFERENCE_NAME = 3;
  private static final int FIELD_SAVE_PREFERENCES_PREFERENCE_VALUE = 4;
}
