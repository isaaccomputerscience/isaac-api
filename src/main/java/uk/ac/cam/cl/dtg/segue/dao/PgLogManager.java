/**
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.inject.name.Named;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.LogEvent;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * @author sac92
 *
 */
public class PgLogManager implements ILogManager {
    private static final Logger log = LoggerFactory.getLogger(PgLogManager.class);

    private final PostgresSqlDb database;
    private final LocationHistoryManager locationManager;
    private final boolean loggingEnabled;
    private final ObjectMapper objectMapper;

    /**
     * PgLogManager.
     * 
     * @param database
     *            client for postgres.
     * @param objectMapper
     *            - so we can map event details to and from json
     * @param loggingEnabled
     *            - whether the log event should be persisted or not?
     * @param locationManager
     *            - Helps identify a rough location for an ip address.
     */
    public PgLogManager(final PostgresSqlDb database, final ObjectMapper objectMapper,
            @Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled,
            final LocationHistoryManager locationManager) {

        this.database = database;
        this.objectMapper = objectMapper;
        this.loggingEnabled = loggingEnabled;
        this.locationManager = locationManager;
    }

    @Override
    public void logEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest,
            final String eventType, final Object eventDetails) {
        Validate.notNull(user);
        try {
            if (user instanceof RegisteredUserDTO) {
                this.persistLogEvent(((RegisteredUserDTO) user).getId().toString(), null, eventType, eventDetails,
                        getClientIpAddr(httpRequest));
            } else {
                this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType, eventDetails,
                        getClientIpAddr(httpRequest));
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to serialize eventDetails as json string", e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to save log event to the databasse", e);
        }
    }

    @Override
    public void logInternalEvent(final AbstractSegueUserDTO user, final String eventType, final Object eventDetails) {
        Validate.notNull(user);
        try {
            if (user instanceof RegisteredUserDTO) {
                this.persistLogEvent(((RegisteredUserDTO) user).getId().toString(), null, eventType, eventDetails,
                        null);
            } else {
                this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType, eventDetails, null);
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to serialize eventDetails as json string", e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to save log event to the databasse", e);
        }
    }

    @Override
    public void transferLogEventsToRegisteredUser(final String oldUserId, final String newUserId) {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("UPDATE logged_events SET user_id = ?, anonymous_user = TRUE"
                    + "WHERE user_id = ?;");

            pst.setString(1, newUserId);
            pst.setString(2, oldUserId);

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException(String.format("Unable to transfer log events from (%s) to (%s)",
                        oldUserId, newUserId));
            }

        } catch (SQLException | SegueDatabaseException e) {
            log.error("Unable to transfer log events", e);
        }
    }

    @Override
    public Collection<LogEvent> getLogsByType(final String type) throws SegueDatabaseException {
        return this.getLogsByUserAndType(type, null, null, null);
    }

    @Override
    public Collection<LogEvent> getLogsByType(final String type, final Date fromDate) throws SegueDatabaseException {
        return this.getLogsByUserAndType(type, fromDate, null, null);
    }

    @Override
    public Collection<LogEvent> getLogsByType(final String type, final Date fromDate, final Date toDate)
            throws SegueDatabaseException {
        return this.getLogsByUserAndType(type, fromDate, toDate, null);
    }

    @Override
    public Long getLogCountByType(final String type) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select COUNT(*) AS TOTAL FROM logged_events WHERE event_type = ?");
            pst.setString(1, type);

            ResultSet results = pst.executeQuery();
            results.next();
            return results.getLong("TOTAL");
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception: Unable to count log events by type", e);
        }
    }

    @Override
    public Collection<LogEvent> getLogsByType(final String type, final Date fromDate, final Date toDate,
            final List<RegisteredUserDTO> usersOfInterest) throws SegueDatabaseException {

        List<String> usersIdsList = Lists.newArrayList();
        for (RegisteredUserDTO u : usersOfInterest) {
            usersIdsList.add(u.getId().toString());
        }

        return this.getLogsByUserAndType(type, fromDate, toDate, usersIdsList);
    }

    @Override
    public Map<String, Map<LocalDate, Integer>> getLogCountByDate(final Collection<String> eventTypes,
            final Date fromDate, final Date toDate, final List<RegisteredUserDTO> usersOfInterest,
            final boolean binDataByMonth) throws SegueDatabaseException {
        Validate.notNull(eventTypes);

        List<String> usersIdsList = Lists.newArrayList();
        if (usersOfInterest != null) {
            for (RegisteredUserDTO u : usersOfInterest) {
                usersIdsList.add(u.getId().toString());
            }
        }

        Map<String, Map<LocalDate, Integer>> result = Maps.newHashMap();

        for (String typeOfInterest : eventTypes) {
            Collection<LogEvent> rs = this.getLogsByUserAndType(typeOfInterest, fromDate, toDate, usersIdsList);

            if (!result.containsKey(typeOfInterest)) {
                result.put(typeOfInterest, new HashMap<LocalDate, Integer>());
            }

            for (LogEvent le : rs) {
                LocalDate dateGroup = this.getDateGroup(le, binDataByMonth);

                if (result.get(typeOfInterest).containsKey(dateGroup)) {
                    result.get(typeOfInterest).put(dateGroup, result.get(typeOfInterest).get(dateGroup) + 1);
                } else {
                    result.get(typeOfInterest).put(dateGroup, 1);
                }
            }
        }

        return result;
    }

    @Override
    public Set<String> getAllIpAddresses() {
        Set<String> ipAddresses = Sets.newHashSet();
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT DISTINCT ip_address FROM logged_events");

            ResultSet results = pst.executeQuery();

            while (results.next()) {
                ipAddresses.add(results.getString("ip_address"));
            }

        } catch (SQLException e) {
            log.error("Unable to get all ip addresses due to a database error.", e);

        }

        return ipAddresses;
    }

    @Override
    public Map<String, LogEvent> getLastLogForAllUsers(final String qualifyingLogEventType)
            throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT DISTINCT ON (user_id) *" + "FROM logged_events WHERE event_type = ? "
                    + "ORDER BY user_id, id DESC;");
            pst.setString(1, qualifyingLogEventType);

            ResultSet results = pst.executeQuery();
            Map<String, LogEvent> resultToReturn = Maps.newHashMap();

            while (results.next()) {
                resultToReturn.put(results.getString("user_id"), this.buildPgLogEventFromPgResult(results));
            }

            return resultToReturn;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find last log for all users", e);
        }
    }

    @Override
    public Set<String> getAllEventTypes() throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT DISTINCT ON (event_type) event_type" + " FROM logged_events");

            ResultSet results = pst.executeQuery();
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
     * Creates a log event from a pg results set..
     * 
     * @param results
     *            - result set containing the informaiton about the log event.
     * @return a log event
     * @throws SQLException
     *             if we cannot read the requested column.
     */
    private LogEvent buildPgLogEventFromPgResult(final ResultSet results) throws SQLException {
        return new LogEvent(results.getString("event_type"), results.getString("event_details_type"),
                results.getObject("event_details"), results.getString("user_id"),
                results.getBoolean("anonymous_user"), results.getString("user_id"), results.getDate("timestamp"));
    }

    /**
     * getLogsByUserAndType.
     * 
     * @param type
     *            - type of log event to search for.
     * @param fromDate
     *            - the earliest date the log event can have occurred
     * @param toDate
     *            - the latest date the log event can have occurred
     * @param userIds
     *            - the list of users ids we are interested in.
     * @return a collection of log events that match the above criteria or an empty collection.
     * @throws SegueDatabaseException
     *             - if we cannot retrieve the data from the database.
     */
    private Collection<LogEvent> getLogsByUserAndType(final String type, final Date fromDate, final Date toDate,
            final Collection<String> userIds) throws SegueDatabaseException {

        String query = "Select * FROM logged_events WHERE event_type = ?";

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

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement(query);
            pst.setString(1, type);

            int index = 2;

            if (fromDate != null) {
                pst.setTimestamp(index++, new java.sql.Timestamp(fromDate.getTime()));
            }
            if (toDate != null) {
                pst.setTimestamp(index++, new java.sql.Timestamp(toDate.getTime()));
            }

            if (userIds != null) {
                for (String userId : userIds) {
                    pst.setString(index++, userId);
                }
            }

            ResultSet results = pst.executeQuery();

            List<LogEvent> returnResult = Lists.newArrayList();
            while (results.next()) {
                returnResult.add(buildPgLogEventFromPgResult(results));
            }

            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * log an event in the database.
     * 
     * @param userId
     *            -
     * @param anonymousUserId
     *            -
     * @param eventType
     *            -
     * @param eventDetails
     *            -
     * @param ipAddress
     *            -
     * @throws JsonProcessingException
     *             - if we are unable to serialize the eventDetails as a string.
     * @throws SegueDatabaseException
     */
    private void persistLogEvent(final String userId, final String anonymousUserId, final String eventType,
            final Object eventDetails, final String ipAddress) throws JsonProcessingException, SegueDatabaseException {
        // don't do anything if logging is not enabled.
        if (!this.loggingEnabled) {
            return;
        }

        LogEvent logEvent = this.buildLogEvent(userId, anonymousUserId, eventType, eventDetails, ipAddress);

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("INSERT INTO logged_events"
                    + "(user_id, anonymous_user, event_type, event_details_type, event_details, "
                    + "ip_address, timestamp) " + "VALUES (?, ?, ?, ?, ?::text::jsonb, ?::inet, ?);",
                    Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, logEvent.getUserId());
            pst.setBoolean(2, logEvent.isAnonymousUser());
            pst.setString(3, logEvent.getEventType());
            pst.setString(4, logEvent.getEventDetailsType());
            pst.setString(5, objectMapper.writeValueAsString(logEvent.getEventDetails()));
            pst.setString(6, logEvent.getIpAddress());
            pst.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));

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
     * Extract client ip address.
     * 
     * Solution retrieved from:
     * http://stackoverflow.com/questions/4678797/how-do-i-get-the-remote-address-of-a-client-in-servlet
     * 
     * @param request
     *            - to attempt to extract a valid Ip from.
     * @return string representation of the client's ip address.
     */
    private static String getClientIpAddr(final HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Generate a logEvent object.
     * 
     * @param userId
     *            - owner user id
     * @param anonymousUserId
     *            - id to use if not logged in
     * @param eventType
     *            - the type of event that has occurred
     * @param eventDetails
     *            - event details if further details are required.
     * @param ipAddress
     *            - the ip address of the client making the request
     * @return a log event.
     * @throws JsonProcessingException
     *             - if we cannot process the json
     * @throws SegueDatabaseException
     *             - If we cannot record the ip address location information
     */
    private LogEvent buildLogEvent(final String userId, final String anonymousUserId, final String eventType,
            final Object eventDetails, final String ipAddress) throws JsonProcessingException, SegueDatabaseException {
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

            try {
                // split based on the fact that we usually get ip addresses of the form
                // [user_ip], [balancer/gateway_ip]
                locationManager.refreshLocation(ipAddress.split(",")[0]);
            } catch (SegueDatabaseException | IOException e1) {
                log.error("Unable to record location information for ip Address: " + ipAddress, e1);
            }
        }

        logEvent.setTimestamp(new Date());

        return logEvent;
    }

    /**
     * Get a date object that is configured correctly.
     * 
     * @param log
     *            containing a valid timestamp
     * @param binDataByMonth
     *            whether we should bin the date by month or not.
     * @return the local date either as per the log event or with the day of the month set to 1.
     */
    private LocalDate getDateGroup(final LogEvent log, final boolean binDataByMonth) {
        LocalDate dateGroup;
        if (binDataByMonth) {
            Calendar logDate = new GregorianCalendar();
            logDate.setTime(log.getTimestamp());
            logDate.set(Calendar.DAY_OF_MONTH, 1);

            dateGroup = new LocalDate(logDate.getTime());
        } else {
            dateGroup = new LocalDate(log.getTimestamp());
        }
        return dateGroup;
    }
}
