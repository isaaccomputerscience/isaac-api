/*
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
package uk.ac.cam.cl.dtg.isaac.dos.eventbookings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * PgEventBookings.
 * 
 * Postgres aware EventBookings.
 *
 */
public class PgEventBookings implements EventBookings {
    private static final Logger log = LoggerFactory.getLogger(PgEventBookings.class);
    private final PostgresSqlDb ds;

    private final ObjectMapper objectMapper;
    private static final String TABLE_NAME = "event_bookings";

    /**
     * 
     * @param ds
     *            connection to the database.
     * @param mapper
     *            object mapper
     */
    public PgEventBookings(final PostgresSqlDb ds, final ObjectMapper mapper) {
        this.ds = ds;
        this.objectMapper = mapper;
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings#add(uk.ac.cam.
     * cl.dtg.isaac.dos.eventbookings.EventBooking)
     */
    @Override
    public EventBooking add(final String eventId, final Long userId, final Long reserveById, final BookingStatus status, Map<String, String> additionalEventInformation) throws SegueDatabaseException {
        if (null == additionalEventInformation) {
            additionalEventInformation = Maps.newHashMap();
        }

        String query = "INSERT INTO event_bookings (id, user_id, reserved_by, event_id, status, created, updated, additional_booking_information)" +
                " VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?::text::jsonb)";
        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
         ) {
            Date creationDate = new Date();
            pst.setLong(1, userId);
            if (reserveById == null) {
                pst.setNull(2, Types.INTEGER);
            } else {
                pst.setLong(2, reserveById);
            }
            pst.setString(3, eventId);
            pst.setString(4, status.name());
            pst.setTimestamp(5, new java.sql.Timestamp(creationDate.getTime()));
            pst.setTimestamp(6, new java.sql.Timestamp(creationDate.getTime()));
            pst.setString(7, objectMapper.writeValueAsString(additionalEventInformation));
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save event booking.");
            }

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    return new PgEventBooking(ds, id, userId, reserveById, eventId, status, creationDate, creationDate, additionalEventInformation);
                } else {
                    throw new SQLException("Creating event booking failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to convert json to string for persistence.", e);
        }
    }

    @Override
    public EventBooking add(final String eventId, final Long userId, final BookingStatus status, Map<String, String> additionalEventInformation) throws SegueDatabaseException {
        return add(eventId, userId, null, status, additionalEventInformation);
    }

    @Override
    public void updateStatus(final String eventId, final Long userId, final Long reservingUserId, final BookingStatus status, final Map<String, String> additionalEventInformation) throws SegueDatabaseException {
        PreparedStatement pst;
        // FIXME: try-with-resources!
        try (Connection conn = ds.getDatabaseConnection()) {

            String reservingUserIdClause = "";
            if (reservingUserId != null) {
                reservingUserIdClause = ", reserved_by = ? ";
            }

            if (additionalEventInformation != null) {
                pst = conn.prepareStatement("UPDATE event_bookings " +
                    "SET status = ?, updated = ?, additional_booking_information = ?::text::jsonb " + reservingUserIdClause +
                    "WHERE event_id = ? AND user_id = ?;");
                pst.setString(1, status.name());
                pst.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
                pst.setString(3, objectMapper.writeValueAsString(additionalEventInformation));
                if (reservingUserId != null) {
                    pst.setLong(4, reservingUserId);
                    pst.setString(5, eventId);
                    pst.setLong(6, userId);
                } else {
                    pst.setString(4, eventId);
                    pst.setLong(5, userId);
                }
            } else {
                pst = conn.prepareStatement("UPDATE event_bookings " +
                    "SET status = ?, updated = ? " + reservingUserIdClause +
                    "WHERE event_id = ? AND user_id = ?;");
                pst.setString(1, status.name());
                pst.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
                if (reservingUserId != null) {
                    pst.setLong(3, reservingUserId);
                    pst.setString(4, eventId);
                    pst.setLong(5, userId);
                } else {
                    pst.setString(3, eventId);
                    pst.setLong(4, userId);
                }
            }

            int executeUpdate = pst.executeUpdate();

            if (executeUpdate == 0) {
                throw new ResourceNotFoundException("Could not delete the requested booking.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while trying to update event booking", e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to convert json to string for persistence.", e);
        }
    }

    @Override
    public void delete(final String eventId, final Long userId) throws SegueDatabaseException {
        String query = "DELETE FROM event_bookings WHERE event_id = ? AND user_id = ?";
        try (Connection conn = ds.getDatabaseConnection();
            PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, eventId);
            pst.setLong(2, userId);
            int executeUpdate = pst.executeUpdate();

            if (executeUpdate == 0) {
                throw new ResourceNotFoundException("Could not delete the requested booking.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while trying to delete event booking", e);
        }
    }

    @Override
    public void deleteAdditionalInformation(Long userId) throws SegueDatabaseException {
        String query = "UPDATE event_bookings SET additional_booking_information = null WHERE user_id = ?;";
        try (Connection conn = ds.getDatabaseConnection();
            PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while trying to expunge additional event information", e);
        }
    }

    /**
     * Acquire a globally unique database lock.
     * This method will block until the lock is released.
     * Any locks must be released manually.
     *
     * @param resourceId - the unique id for the object to be locked.
     */
    @Override
    public void acquireDistributedLock(final String resourceId) throws SegueDatabaseException {
        // generate 32 bit CRC based on table id and resource id so that is is more likely to be unique globally.
        CRC32 crc = new CRC32();
        crc.update((TABLE_NAME + resourceId).getBytes());

        // acquire lock
        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT pg_advisory_lock(?)");
         ) {
            pst.setLong(1, crc.getValue());
            log.debug(String.format("Acquiring advisory lock on %s (%s)", TABLE_NAME + resourceId, crc.getValue()));
            pst.executeQuery();
        } catch (SQLException e) {
            String msg = String.format(
                "Unable to acquire lock for event (%s).", resourceId);
            log.error(msg);
            throw new SegueDatabaseException(msg);
        }
        log.debug(String.format("Acquired advisory lock on %s (%s)", TABLE_NAME + resourceId, crc.getValue()));
    }

    /**
     * Release a globally unique database lock.
     * This method will release a previously acquired lock.
     *
     * @param resourceId - the unique id for the object to be locked.
     */
    @Override
    public void releaseDistributedLock(final String resourceId) throws SegueDatabaseException {

        // generate 32 bit CRC based on table id and resource id so that is is more likely to be unique globally.
        CRC32 crc = new CRC32();
        crc.update((TABLE_NAME + resourceId).getBytes());

        // acquire lock
        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT pg_advisory_unlock(?)");
        ) {
            pst.setLong(1, crc.getValue());
            log.debug(String.format("Releasing advisory lock on %s (%s)", TABLE_NAME + resourceId, crc.getValue()));
            pst.executeQuery();
        } catch (SQLException e) {
            String msg = String.format(
                "Unable to release lock for event (%s).", resourceId);
            log.error(msg);
            throw new SegueDatabaseException(msg);
        }
        log.debug(String.format("Released advisory lock on %s (%s)", TABLE_NAME + resourceId, crc.getValue()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings#iterate()
     */
    @Override
    public EventBooking findBookingByEventAndUser(final String eventId, final Long userId)
            throws SegueDatabaseException {
        Validate.notBlank(eventId);

        String query = "SELECT * FROM event_bookings WHERE event_id = ? AND user_id = ?";
        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, eventId);
            pst.setLong(2, userId);

            try (ResultSet results = pst.executeQuery()) {
                EventBooking result = null;
                int count = 0;
                while (results.next()) {
                    result = buildPgEventBooking(results);
                    count++;
                }

                if (count == 1) {
                    return result;
                } else if (count == 0) {
                    throw new ResourceNotFoundException("Unable to locate the booking you requested.");
                } else {
                    String msg = String.format(
                            "Found more than one event booking that matches event id (%s) and user id (%s).", eventId,
                            userId);
                    log.error(msg);
                    throw new SegueDatabaseException(msg);
                }
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings#iterate()
     */
    @Override
    public Iterable<EventBooking> findAllByEventId(final String eventId) throws SegueDatabaseException {
        return this.findAllByEventIdAndStatus(eventId, (BookingStatus) null);
    }

    @Override
    public Long countAllEventBookings() throws SegueDatabaseException {
        String query = "SELECT COUNT(1) AS TOTAL FROM event_bookings";
        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
             ResultSet results = pst.executeQuery();
        ) {
            results.next();
            return results.getLong("TOTAL");
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Map<BookingStatus, Map<Role, Long>> getEventBookingStatusCounts(final String eventId, final boolean includeDeletedUsersInCounts) throws SegueDatabaseException {
        // Note this method joins at the db table mainly to allow inclusion of deleted users in the counts.
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT event_bookings.status, users.role, COUNT(event_bookings.id) FROM event_bookings \n" +
                "INNER JOIN users ON event_bookings.user_id = users.id\n" +
                "WHERE event_bookings.event_id=?"
        );

        if (!includeDeletedUsersInCounts) {
            sb.append(" AND users.deleted = 'f'\n" );
        }

        sb.append(" GROUP BY event_bookings.status, users.role;");

        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(sb.toString());
        ) {
            pst.setString(1, eventId);

            try (ResultSet results = pst.executeQuery()) {
                Map<BookingStatus, Map<Role, Long>> returnResult = Maps.newHashMap();
                while (results.next()) {
                    BookingStatus bookingStatus = BookingStatus.valueOf(results.getString("status"));
                    Role role = Role.valueOf(results.getString("role"));
                    Long count = results.getLong("count");

                    Map<Role, Long> roleCountMap = returnResult.getOrDefault(bookingStatus, Maps.newHashMap());
                    roleCountMap.put(role, count);
                    returnResult.put(bookingStatus, roleCountMap);
                }
                return returnResult;
            }
        } catch (SQLException e) {
            log.error("DB error ", e);
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * Find all bookings for a given event with a given status.
     * <p>
     * Useful for finding all on a waiting list or confirmed.
     *
     * @param eventId - the event of interest.
     * @param status  - The event status that should match in the bookings returned. Can be null
     * @return an iterable with all the events matching the criteria.
     * @throws SegueDatabaseException - if an error occurs.
     */
    @Override
    public Iterable<EventBooking> findAllByEventIdAndStatus(final String eventId, @Nullable final BookingStatus status) throws SegueDatabaseException {
        Validate.notBlank(eventId);

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM event_bookings WHERE event_id = ?");

        if (status != null) {
            sb.append(" AND status = ?");
        }

        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(sb.toString());
        ) {
            pst.setString(1, eventId);
            if (status != null) {
                pst.setString(2, status.name());
            }

            try (ResultSet results = pst.executeQuery()) {
                List<EventBooking> returnResult = Lists.newArrayList();
                while (results.next()) {
                    returnResult.add(buildPgEventBooking(results));
                }
                return returnResult;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Iterable<EventBooking> findAllByUserId(final Long userId) throws SegueDatabaseException {
        Validate.notNull(userId);

        String query = "SELECT * FROM event_bookings WHERE user_id = ?";
        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);

            try (ResultSet results = pst.executeQuery()) {
                List<EventBooking> returnResult = Lists.newArrayList();
                while (results.next()) {
                    returnResult.add(buildPgEventBooking(results));
                }
                return returnResult;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Iterable<EventBooking> findAllReservationsByUserId(final Long userId) throws SegueDatabaseException {
        Validate.notNull(userId);

        String query = "SELECT distinct on (event_id) * FROM event_bookings WHERE reserved_by = ? AND status != 'CANCELLED'";
        try (Connection conn = ds.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);

            try (ResultSet results = pst.executeQuery()) {
                List<EventBooking> returnResult = Lists.newArrayList();
                while (results.next()) {
                    returnResult.add(buildPgEventBooking(results));
                }
                return returnResult;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * Create a pgEventBooking from a results set.
     * 
     * Assumes there is a result to read.
     * 
     * @param results
     *            - the results to convert
     * @return a new PgEventBooking
     * @throws SQLException
     *             - if an error occurs.
     */
    private PgEventBooking buildPgEventBooking(final ResultSet results) throws SQLException, SegueDatabaseException {
        return new PgEventBooking(ds,
                results.getLong("id"),
                results.getLong("user_id"),
                results.getLong("reserved_by"),
                results.getString("event_id"),
                BookingStatus.valueOf(results.getString("status")),
                results.getTimestamp("created"),
                results.getTimestamp("updated"),
                results.getObject("additional_booking_information")
        );
    }
}
