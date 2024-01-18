package uk.ac.cam.cl.dtg.isaac.dos.eventbookings;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
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
}
