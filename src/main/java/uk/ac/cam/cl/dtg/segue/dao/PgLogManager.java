/**
 * Copyright 2015 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.dao;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.ALL_ACCEPTED_LOG_TYPES;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.LOG_EVENT;
import static uk.ac.cam.cl.dtg.segue.dao.AbstractPgDataManager.getInstantFromDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.LogEvent;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.LogType;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.RequestIpExtractor;

public class PgLogManager implements ILogManager {
  private static final Logger log = LoggerFactory.getLogger(PgLogManager.class);

  private final PostgresSqlDb database;
  private final boolean loggingEnabled;
  private final ObjectMapper objectMapper;

  /**
   * PgLogManager.
   *
   * @param database       client for postgres.
   * @param objectMapper   so we can map event details to and from json
   * @param loggingEnabled whether the log event should be persisted or not?
   */
  @Inject
  public PgLogManager(final PostgresSqlDb database, final ObjectMapper objectMapper,
                      @Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled) {

    this.database = database;
    this.objectMapper = objectMapper;
    this.loggingEnabled = loggingEnabled;
  }

  @Override
  public void logEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest, final LogType eventType,
                       final Object eventDetails) {
    requireNonNull(user);
    try {
      if (user instanceof RegisteredUserDTO registeredUserDTO) {
        this.persistLogEvent(registeredUserDTO.getId().toString(), null, eventType.name(), eventDetails,
            RequestIpExtractor.getClientIpAddr(httpRequest));
      } else {
        this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType.name(), eventDetails,
            RequestIpExtractor.getClientIpAddr(httpRequest));
      }

    } catch (JsonProcessingException e) {
      log.error("Unable to serialize eventDetails as json string", e);
    } catch (SegueDatabaseException e) {
      log.error("Unable to save log event to the database", e);
    }
  }

  @Override
  public void logExternalEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest,
                               final String eventType, final Object eventDetails) {
    requireNonNull(user);
    try {
      if (user instanceof RegisteredUserDTO registeredUserDTO) {
        this.persistLogEvent(registeredUserDTO.getId().toString(), null, eventType, eventDetails,
            RequestIpExtractor.getClientIpAddr(httpRequest));
      } else {
        this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType, eventDetails,
            RequestIpExtractor.getClientIpAddr(httpRequest));
      }

    } catch (JsonProcessingException e) {
      log.error("Unable to serialize eventDetails as json string", e);
    } catch (SegueDatabaseException e) {
      log.error("Unable to save log event to the database", e);
    }
  }

  @Override
  public void logInternalEvent(final AbstractSegueUserDTO user, final LogType eventType, final Object eventDetails) {
    requireNonNull(user);
    try {
      if (user instanceof RegisteredUserDTO registeredUserDTO) {
        this.persistLogEvent(registeredUserDTO.getId().toString(), null, eventType.name(), eventDetails,
            null);
      } else {
        this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType.name(), eventDetails, null);
      }

    } catch (JsonProcessingException e) {
      log.error("Unable to serialize eventDetails as json string", e);
    } catch (SegueDatabaseException e) {
      log.error("Unable to save log event to the databasse", e);
    }
  }

  @Override
  public void transferLogEventsToRegisteredUser(final String oldUserId, final String newUserId) {
    String query = "UPDATE logged_events SET user_id = ?, anonymous_user = TRUE WHERE user_id = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_TRANSFER_LOG_EVENTS_NEW_USER_ID, newUserId);
      pst.setString(FIELD_TRANSFER_LOG_EVENTS_OLD_USER_ID, oldUserId);

      pst.executeUpdate();

    } catch (SQLException e) {
      log.error("Unable to transfer log events", e);
    }
  }

  @Override
  public Collection<LogEvent> getLogsByType(final String type, final Instant fromDate, final Instant toDate)
      throws SegueDatabaseException {
    return this.getLogsByUserAndType(type, fromDate, toDate, null);
  }

  @Override
  public Collection<LogEvent> getLogsByType(final String type, final Instant fromDate, final Instant toDate,
                                            final List<RegisteredUserDTO> usersOfInterest)
      throws SegueDatabaseException {

    List<String> usersIdsList = Lists.newArrayList();
    for (RegisteredUserDTO u : usersOfInterest) {
      usersIdsList.add(u.getId().toString());
    }

    return this.getLogsByUserAndType(type, fromDate, toDate, usersIdsList);
  }

  @Override
  public Long getLogCountByType(final String type) throws SegueDatabaseException {
    String query = "SELECT COUNT(*) AS TOTAL FROM logged_events WHERE event_type = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_LOG_COUNT_EVENT_TYPE, type);

      try (ResultSet results = pst.executeQuery()) {
        results.next();
        return results.getLong("TOTAL");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception: Unable to count log events by type", e);
    }
  }

  @Override
  public Map<String, Map<LocalDate, Long>> getLogCountByDate(final Collection<String> eventTypes,
                                                             final Instant fromDate, final Instant toDate,
                                                             final List<RegisteredUserDTO> usersOfInterest,
                                                             final boolean binDataByMonth)
      throws SegueDatabaseException {
    requireNonNull(eventTypes);

    List<String> usersIdsList = Lists.newArrayList();
    if (usersOfInterest != null) {
      for (RegisteredUserDTO u : usersOfInterest) {
        usersIdsList.add(u.getId().toString());
      }
    }

    Map<String, Map<LocalDate, Long>> result = Maps.newHashMap();

    for (String typeOfInterest : eventTypes) {
      Map<Instant, Long> rs = this.getLogsCountByMonthFilteredByUserAndType(typeOfInterest, fromDate, toDate,
          usersIdsList);

      if (!result.containsKey(typeOfInterest)) {
        result.put(typeOfInterest, new HashMap<LocalDate, Long>());
      }

      for (Entry<Instant, Long> le : rs.entrySet()) {
        LocalDate localisedDate = LocalDate.from(le.getKey());

        if (result.get(typeOfInterest).containsKey(localisedDate)) {
          result.get(typeOfInterest).put(localisedDate,
              result.get(typeOfInterest).get(localisedDate) + le.getValue());
        } else {
          result.get(typeOfInterest).put(localisedDate, le.getValue());
        }
      }
    }

    return result;
  }

  @Override
  public Set<String> getAllIpAddresses() {
    Set<String> ipAddresses = Sets.newHashSet();
    String query = "SELECT DISTINCT ip_address FROM logged_events";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query);
         ResultSet results = pst.executeQuery()
    ) {
      while (results.next()) {
        ipAddresses.add(results.getString("ip_address"));
      }

    } catch (SQLException e) {
      log.error("Unable to get all ip addresses due to a database error.", e);

    }

    return ipAddresses;
  }

  @Override
  public Map<String, Instant> getLastLogDateForAllUsers(final String qualifyingLogEventType)
      throws SegueDatabaseException {
    String query =
        "SELECT DISTINCT ON (user_id) user_id, \"timestamp\" FROM logged_events WHERE event_type = ?"
            + " ORDER BY user_id, id DESC;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_LOG_DATE_EVENT_TYPE, qualifyingLogEventType);

      try (ResultSet results = pst.executeQuery()) {
        Map<String, Instant> resultToReturn = Maps.newHashMap();

        while (results.next()) {
          resultToReturn.put(results.getString("user_id"), getInstantFromDate(results, "timestamp"));
        }

        return resultToReturn;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find last log for all users", e);
    }
  }

  @Override
  public Set<String> getAllEventTypes() throws SegueDatabaseException {
    String query = "SELECT event_type FROM logged_events GROUP BY event_type";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query);
         ResultSet results = pst.executeQuery()
    ) {
      Set<String> eventTypesRecorded = Sets.newHashSet();

      while (results.next()) {
        eventTypesRecorded.add(results.getString("event_type"));
      }

      return eventTypesRecorded;
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find event types", e);
    }
  }

  /**
   * Creates a log event from a pg results set.
   *
   * @param results  result set containing the information about the log event.
   * @return a log event
   * @throws SQLException if we cannot read the requested column.
   */
  private LogEvent buildPgLogEventFromPgResult(final ResultSet results) throws SQLException {
    return new LogEvent(results.getString("event_type"), results.getString("event_details_type"),
        results.getObject("event_details"), results.getString("user_id"), results.getBoolean("anonymous_user"),
        results.getString("user_id"), getInstantFromDate(results, "timestamp"));
  }

  /**
   * getLogsCountByMonthFilteredByUserAndType.
   * <br>
   * An optimised method for getting log counts data by month.
   * This relies on the database doing the binning for us.
   *
   * @param type     type of log event to search for.
   * @param fromDate the earliest date the log event can have occurred
   * @param toDate   the latest date the log event can have occurred
   * @param userIds  the list of users ids we are interested in.
   * @return a collection of log events that match the above criteria or an empty collection.
   * @throws SegueDatabaseException if we cannot retrieve the data from the database.
   */
  private Map<Instant, Long> getLogsCountByMonthFilteredByUserAndType(final String type, final Instant fromDate,
                                                                      final Instant toDate,
                                                                      final Collection<String> userIds)
      throws SegueDatabaseException {
    requireNonNull(fromDate);
    requireNonNull(toDate);

    StringBuilder queryToBuild = new StringBuilder();
    queryToBuild.append("WITH filtered_logs AS (SELECT * FROM logged_events WHERE event_type=?");
    if (userIds != null && !userIds.isEmpty()) {
      StringBuilder inParams = new StringBuilder();
      inParams.append("?");
      for (int i = 1; i < userIds.size(); i++) {
        inParams.append(",?");
      }

      queryToBuild.append(String.format(" AND user_id IN (%s)", inParams.toString()));

    }
    queryToBuild.append(") ");
    // The following LEFT JOIN gives us months with no events in as required, but need count(id) not count(1) to
    // count actual logged events (where id strictly NOT NULL) in those months, and not count an extra '1' for
    // empty months where id is NULL by definition of the JOIN.
    queryToBuild.append("SELECT to_char(gen_month, 'YYYY-MM-01'), count(id)");
    queryToBuild.append(" FROM generate_series(date_trunc('month', ?::timestamp), ?, INTERVAL '1' MONTH) m(gen_month)");
    queryToBuild.append(
        " LEFT OUTER JOIN filtered_logs ON ( date_trunc('month', \"timestamp\") = date_trunc('month', gen_month) )");
    queryToBuild.append(" GROUP BY gen_month ORDER BY gen_month ASC;");

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(queryToBuild.toString())
    ) {
      pst.setString(FIELD_GET_LOG_COUNT_BY_MONTH_FILTERED_EVENT_TYPE, type);

      int index = GET_LOG_COUNT_BY_MONTH_FILTERED_FIRST_USER_ID_OR_TIMESTAMP_INDEX;
      if (userIds != null) {
        for (String userId : userIds) {
          pst.setString(index++, userId);
        }
      }
      pst.setTimestamp(index++, Timestamp.from(fromDate));
      pst.setTimestamp(index++, Timestamp.from(toDate));

      try (ResultSet results = pst.executeQuery()) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(UTC);

        Map<Instant, Long> mapToReturn = Maps.newHashMap();
        while (results.next()) {
          Instant parsedDate =
              formatter.parse(results.getString("to_char"), LocalDate::from).atStartOfDay(UTC).toInstant();
          mapToReturn.put(parsedDate, results.getLong("count"));
        }

        return mapToReturn;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    } catch (DateTimeParseException e) {
      throw new SegueDatabaseException("Unable to parse date exception", e);
    }
  }

  /**
   * getLogsByUserAndType.
   * <br>
   * WARNING: This should be used with care. Do not request too much
   * TODO: add pagination
   *
   * @param type     type of log event to search for.
   * @param fromDate the earliest date the log event can have occurred
   * @param toDate   the latest date the log event can have occurred
   * @param userIds  the list of users ids we are interested in.
   * @return a collection of log events that match the above criteria or an empty collection.
   * @throws SegueDatabaseException if we cannot retrieve the data from the database.
   */
  private Collection<LogEvent> getLogsByUserAndType(final String type, final Instant fromDate, final Instant toDate,
                                                    final Collection<String> userIds) throws SegueDatabaseException {

    String query = "SELECT * FROM logged_events WHERE event_type = ?";

    if (fromDate != null) {
      query += " AND timestamp > ?";
    }

    if (toDate != null) {
      query += " AND timestamp < ?";
    }

    if (userIds != null && !userIds.isEmpty()) {
      StringBuilder inParams = new StringBuilder();
      inParams.append("?");
      for (int i = 1; i < userIds.size(); i++) {
        inParams.append(",?");
      }

      query += String.format(" AND user_id IN (%s)", inParams.toString());

    }

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_LOGS_BY_USER_AND_TYPE_EVENT_TYPE, type);

      int index = GET_LOGS_BY_USER_AND_TYPE_FIRST_USER_ID_OR_TIMESTAMP_INDEX;

      if (fromDate != null) {
        pst.setTimestamp(index++, Timestamp.from(fromDate));
      }
      if (toDate != null) {
        pst.setTimestamp(index++, Timestamp.from(toDate));
      }

      if (userIds != null) {
        for (String userId : userIds) {
          pst.setString(index++, userId);
        }
      }

      try (ResultSet results = pst.executeQuery()) {

        List<LogEvent> returnResult = Lists.newArrayList();
        while (results.next()) {
          returnResult.add(buildPgLogEventFromPgResult(results));
        }

        return returnResult;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * log an event in the database.
   *
   * @param userId          owner user id
   * @param anonymousUserId id to use if not logged in
   * @param eventType       the type of event that has occurred
   * @param eventDetails    the type of event that has occurred
   * @param ipAddress       the ip address of the client making the request
   * @throws JsonProcessingException if we are unable to serialize the eventDetails as a string.
   * @throws SegueDatabaseException  if we cannot persist the event in the database.
   */
  private void persistLogEvent(final String userId, final String anonymousUserId, final String eventType,
                               final Object eventDetails, final String ipAddress)
      throws JsonProcessingException, SegueDatabaseException {
    // don't do anything if logging is not enabled.
    if (!this.loggingEnabled) {
      return;
    }

    LogEvent logEvent = this.buildLogEvent(userId, anonymousUserId, eventType, eventDetails, ipAddress);

    // Record log event occurrence for internal metrics
    if (ALL_ACCEPTED_LOG_TYPES.contains(eventType)) {
      LOG_EVENT.labels(eventType).inc();
    }

    String query = "INSERT INTO logged_events(user_id, anonymous_user, event_type, event_details_type,"
        + " event_details, ip_address, timestamp) VALUES (?, ?, ?, ?, ?::text::jsonb, ?::inet, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
    ) {
      pst.setString(FIELD_PERSIST_LOG_EVENT_USER_ID, logEvent.getUserId());
      pst.setBoolean(FIELD_PERSIST_LOG_EVENT_IS_ANONYMOUS, logEvent.isAnonymousUser());
      pst.setString(FIELD_PERSIST_LOG_EVENT_EVENT_TYPE, logEvent.getEventType());
      pst.setString(FIELD_PERSIST_LOG_EVENT_EVENT_DETAILS_TYPE, logEvent.getEventDetailsType());
      pst.setString(FIELD_PERSIST_LOG_EVENT_EVENT_DETAILS, objectMapper.writeValueAsString(logEvent.getEventDetails()));
      pst.setString(FIELD_PERSIST_LOG_EVENT_IP_ADDRESS, logEvent.getIpAddress());
      pst.setTimestamp(FIELD_PERSIST_LOG_EVENT_TIMESTAMP, Timestamp.from(Instant.now()));

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save user.");
      }

      try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating user failed, no ID obtained.");
        }
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * Generate a logEvent object.
   *
   * @param userId          owner user id
   * @param anonymousUserId id to use if not logged in
   * @param eventType       the type of event that has occurred
   * @param eventDetails    the type of event that has occurred
   * @param ipAddress       the ip address of the client making the request
   * @return a log event.
   */
  private LogEvent buildLogEvent(final String userId, final String anonymousUserId, final String eventType,
                                 final Object eventDetails, final String ipAddress) {
    if (null == userId && null == anonymousUserId) {
      throw new IllegalArgumentException("UserId or anonymousUserId must be set.");
    }

    LogEvent logEvent = new LogEvent();

    if (null != userId) {
      logEvent.setUserId(userId);
      logEvent.setAnonymousUser(false);
    } else {
      logEvent.setUserId(anonymousUserId);
      logEvent.setAnonymousUser(true);
    }

    logEvent.setEventType(eventType);

    if (eventDetails != null) {
      logEvent.setEventDetailsType(eventDetails.getClass().getCanonicalName());
      logEvent.setEventDetails(eventDetails);
    }

    if (ipAddress != null) {
      logEvent.setIpAddress(ipAddress.split(",")[0]);
    }

    logEvent.setTimestamp(Instant.now());

    return logEvent;
  }

  // Field Constants
  // transferLogEventsToRegisteredUser
  private static final int FIELD_TRANSFER_LOG_EVENTS_NEW_USER_ID = 1;
  private static final int FIELD_TRANSFER_LOG_EVENTS_OLD_USER_ID = 2;

  // getLogCountByType
  private static final int FIELD_GET_LOG_COUNT_EVENT_TYPE = 1;

  // getLastLogDateForAllUsers
  private static final int FIELD_GET_LOG_DATE_EVENT_TYPE = 1;

  // getLogsCountByMonthFilteredByUserAndType
  private static final int FIELD_GET_LOG_COUNT_BY_MONTH_FILTERED_EVENT_TYPE = 1;
  private static final int GET_LOG_COUNT_BY_MONTH_FILTERED_FIRST_USER_ID_OR_TIMESTAMP_INDEX = 2;

  // getLogsByUserAndType
  private static final int FIELD_GET_LOGS_BY_USER_AND_TYPE_EVENT_TYPE = 1;
  private static final int GET_LOGS_BY_USER_AND_TYPE_FIRST_USER_ID_OR_TIMESTAMP_INDEX = 2;

  // persistLogEvent
  private static final int FIELD_PERSIST_LOG_EVENT_USER_ID = 1;
  private static final int FIELD_PERSIST_LOG_EVENT_IS_ANONYMOUS = 2;
  private static final int FIELD_PERSIST_LOG_EVENT_EVENT_TYPE = 3;
  private static final int FIELD_PERSIST_LOG_EVENT_EVENT_DETAILS_TYPE = 4;
  private static final int FIELD_PERSIST_LOG_EVENT_EVENT_DETAILS = 5;
  private static final int FIELD_PERSIST_LOG_EVENT_IP_ADDRESS = 6;
  private static final int FIELD_PERSIST_LOG_EVENT_TIMESTAMP = 7;
}
