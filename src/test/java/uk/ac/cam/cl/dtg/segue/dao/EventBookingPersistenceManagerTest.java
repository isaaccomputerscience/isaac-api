package uk.ac.cam.cl.dtg.segue.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventBookingPersistenceManagerTest {

  private EventBookingManager eventBookingManager;
  private EventBookingPersistenceManager mockEventBookingPersistenceManager;
  private ContentManager mockContentManager;

  @BeforeEach
  public void setUp() {
    mockEventBookingPersistenceManager = createMock(EventBookingPersistenceManager.class);
    mockContentManager = createMock(ContentManager.class);
    eventBookingManager = new EventBookingManager(mockEventBookingPersistenceManager, mockContentManager);
  }

  @Test
  public void testAdminGetBookingsByEventIds() throws SegueDatabaseException {
    // Given
    List<String> eventIds = Arrays.asList("event1", "event2");
    Map<String, ContentDTO> expectedEventDetails = Map.of("event1", new ContentDTO(), "event2", new ContentDTO());
    List<EventBooking> mockBookings = Arrays.asList(new EventBooking(), new EventBooking());

    expect(mockContentManager.getContentById("event1")).andReturn(expectedEventDetails.get("event1"));
    expect(mockContentManager.getContentById("event2")).andReturn(expectedEventDetails.get("event2"));
    expect(mockEventBookingPersistenceManager.findAllByEventIds(eventIds)).andReturn(mockBookings);
    replay(mockEventBookingPersistenceManager, mockContentManager);

    // When
    Map<String, List<DetailedEventBookingDTO>> result = eventBookingManager.adminGetBookingsByEventIds(eventIds);

    // Then
    assertEquals(2, result.size());
    verify(mockEventBookingPersistenceManager, mockContentManager);
  }
}

