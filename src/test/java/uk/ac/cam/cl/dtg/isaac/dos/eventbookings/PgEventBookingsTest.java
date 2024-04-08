package uk.ac.cam.cl.dtg.isaac.dos.eventbookings;

import static java.time.Instant.now;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.ac.cam.cl.dtg.CustomAssertions.assertDeepEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

class PgEventBookingsTest {
  private PgEventBookings buildPgEventBookings() {
    return new PgEventBookings(dummyPostgresSqlDb, dummyObjectMapper);
  }

  private PostgresSqlDb dummyPostgresSqlDb;
  private ObjectMapper dummyObjectMapper;
  private Connection dummyConnection;
  private PreparedStatement dummyPreparedStatement;
  private ResultSet dummyResultSet;

  @BeforeEach
  public final void setUp() throws Exception {
    this.dummyPostgresSqlDb = createMock(PostgresSqlDb.class);
    this.dummyObjectMapper = createMock(ObjectMapper.class);
    this.dummyConnection = createMock(Connection.class);
    this.dummyPreparedStatement = createMock(PreparedStatement.class);
    this.dummyResultSet = createMock(ResultSet.class);
  }

  @Test
  void getEventBookingStatusCounts_checkEventBookingStatusCounts_canCopeWithComplicatedResult()
      throws Exception {
    // Mock setup
    expect(dummyPostgresSqlDb.getDatabaseConnection()).andReturn(dummyConnection).once();
    expect(dummyConnection.prepareStatement(anyString())).andReturn(dummyPreparedStatement).once();
    dummyPreparedStatement.setString(anyInt(), anyString());
    expect(dummyPreparedStatement.executeQuery()).andReturn(dummyResultSet);
    dummyPreparedStatement.close();

    // Mock db result
    expect(dummyResultSet.next()).andReturn(true).once();
    expect(dummyResultSet.getString("status")).andReturn("CONFIRMED").once();
    expect(dummyResultSet.getString("role")).andReturn("STUDENT").once();
    expect(dummyResultSet.getLong("count")).andReturn(20L).once();

    expect(dummyResultSet.next()).andReturn(true).once();
    expect(dummyResultSet.getString("status")).andReturn("CONFIRMED").once();
    expect(dummyResultSet.getString("role")).andReturn("TEACHER").once();
    expect(dummyResultSet.getLong("count")).andReturn(4L).once();

    expect(dummyResultSet.next()).andReturn(true).once();
    expect(dummyResultSet.getString("status")).andReturn("WAITING_LIST").once();
    expect(dummyResultSet.getString("role")).andReturn("STUDENT").once();
    expect(dummyResultSet.getLong("count")).andReturn(10L).once();

    expect(dummyResultSet.next()).andReturn(true).once();
    expect(dummyResultSet.getString("status")).andReturn("CANCELLED").once();
    expect(dummyResultSet.getString("role")).andReturn("TEACHER").once();
    expect(dummyResultSet.getLong("count")).andReturn(2L).once();

    expect(dummyResultSet.next()).andReturn(false).once();
    dummyResultSet.close();
    dummyConnection.close();

    // Create expected status count
    Map<BookingStatus, Map<Role, Long>> expectedStatusCounts = new HashMap<BookingStatus, Map<Role, Long>>() {{
        put(BookingStatus.CONFIRMED, new HashMap<Role, Long>() {{
            put(Role.STUDENT, 20L);
            put(Role.TEACHER, 4L);
          }
        });
        put(BookingStatus.WAITING_LIST, new HashMap<Role, Long>() {{
            put(Role.STUDENT, 10L);
          }
        });
        put(BookingStatus.CANCELLED, new HashMap<Role, Long>() {{
            put(Role.TEACHER, 2L);
          }
        });
      }};

    // Run test
    Object[] mockedObjects = {dummyPostgresSqlDb, dummyConnection, dummyPreparedStatement, dummyResultSet};
    replay(mockedObjects);
    PgEventBookings pgEventBookings = this.buildPgEventBookings();
    Map<BookingStatus, Map<Role, Long>> actualStatusCounts =
        pgEventBookings.getEventBookingStatusCounts("someEventId", true);
    assertEquals(expectedStatusCounts, actualStatusCounts, "Every row should be represented in the result");
    verify(mockedObjects);
  }

  @Test
  void testFindAllByEventIds() throws SQLException, SegueDatabaseException {
    List<String> eventIds = Arrays.asList("event1", "event2");
    String expectedQuery = "SELECT event_bookings.* FROM event_bookings JOIN users ON users.id=user_id WHERE event_id IN (?, ?) AND NOT users.deleted";
    Instant now = now();

    EventBooking expectedBooking1 =
        new PgEventBooking(1L, 1L, 7L, "event1", BookingStatus.CONFIRMED, Date.from(now), Date.from(now), null);
    EventBooking expectedBooking2 =
        new PgEventBooking(2L, 2L, 7L, "event2", BookingStatus.CONFIRMED, Date.from(now), Date.from(now), null);

    expect(dummyPostgresSqlDb.getDatabaseConnection()).andReturn(dummyConnection);
    expect(dummyConnection.prepareStatement(expectedQuery)).andReturn(dummyPreparedStatement);
    dummyPreparedStatement.setString(1, "event1");
    dummyPreparedStatement.setString(2, "event2");
    expect(dummyPreparedStatement.executeQuery()).andReturn(dummyResultSet);

    expect(dummyResultSet.next()).andReturn(true);
    expect(dummyResultSet.getLong("id")).andReturn(1L);
    expect(dummyResultSet.getLong("user_id")).andReturn(1L);
    expect(dummyResultSet.getLong("reserved_by")).andReturn(7L);
    expect(dummyResultSet.getString("event_id")).andReturn("event1");
    expect(dummyResultSet.getString("status")).andReturn("CONFIRMED");
    expect(dummyResultSet.getTimestamp("created")).andReturn(Timestamp.from(now));
    expect(dummyResultSet.getTimestamp("updated")).andReturn(Timestamp.from(now));
    expect(dummyResultSet.getObject("additional_booking_information")).andReturn(null);

    expect(dummyResultSet.next()).andReturn(true);
    expect(dummyResultSet.getLong("id")).andReturn(2L);
    expect(dummyResultSet.getLong("user_id")).andReturn(2L);
    expect(dummyResultSet.getLong("reserved_by")).andReturn(7L);
    expect(dummyResultSet.getString("event_id")).andReturn("event2");
    expect(dummyResultSet.getString("status")).andReturn("CONFIRMED");
    expect(dummyResultSet.getTimestamp("created")).andReturn(Timestamp.from(now));
    expect(dummyResultSet.getTimestamp("updated")).andReturn(Timestamp.from(now));
    expect(dummyResultSet.getObject("additional_booking_information")).andReturn(null);

    expect(dummyResultSet.next()).andReturn(false);
    dummyResultSet.close();
    dummyPreparedStatement.close();
    dummyConnection.close();

    // Create an array of all mocked objects
    Object[] mockedObjects = {dummyPostgresSqlDb, dummyObjectMapper, dummyConnection, dummyPreparedStatement, dummyResultSet};
    replay(mockedObjects);

    PgEventBookings pgEventBookings = this.buildPgEventBookings();
    Iterable<EventBooking> result = pgEventBookings.findAllByEventIds(eventIds);
    assertNotNull(result);

    List<EventBooking> resultList = (List<EventBooking>) result;
    List<EventBooking> expectedEventBookings = Arrays.asList(expectedBooking1, expectedBooking2);
    assertDeepEquals(expectedEventBookings, resultList);

    verify(mockedObjects);
  }
}
