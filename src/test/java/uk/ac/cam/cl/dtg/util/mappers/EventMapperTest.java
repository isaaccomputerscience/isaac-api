package uk.ac.cam.cl.dtg.util.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.util.mappers.MapperTestUtils.assertDeepEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

class EventMapperTest {

  private final EventMapper eventMapper = EventMapper.INSTANCE;
  private static final Date testDate = new Date();

  @ParameterizedTest
  @MethodSource("testCasesFromEventBookingDTO")
  @DisplayName("Test event mapping")
  <T extends EventBookingDTO> void testEventMapping(DetailedEventBookingDTO source, Class<T> targetClass, T expected)
      throws JsonProcessingException {
    T result = eventMapper.map(source, targetClass);
    assertEquals(expected.getClass(), result.getClass());
    assertDeepEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("provideEventBookingDTOTestCases")
  @DisplayName("Test mapping from DetailedEventBookingDTO to EventBookingDTO")
  void mapDetailedEventBookingDTOtoEventBookingDTO_ShouldMapCorrectly(DetailedEventBookingDTO input, EventBookingDTO expected)
      throws JsonProcessingException {
    EventBookingDTO result = eventMapper.mapDetailedEventBookingDTOtoEventBookingDTO(input);
    assertEquals(expected.getClass(), result.getClass());
    assertDeepEquals(expected, result);
  }

  private static Stream<Arguments> testCasesFromEventBookingDTO() {
    return Stream.of(
        Arguments.of(prepareDetailedEventBookingDTO(), EventBookingDTO.class, prepareEventBookingDTO())
    );
  }

  private static Stream<Arguments> provideEventBookingDTOTestCases() {
    return Stream.of(
        Arguments.of(prepareDetailedEventBookingDTO(), prepareEventBookingDTO())
    );
  }

  private static DetailedEventBookingDTO prepareDetailedEventBookingDTO() {
    DetailedEventBookingDTO object = new DetailedEventBookingDTO();
    object.setAdditionalInformation(null);
    return object;
  }

  private static EventBookingDTO prepareEventBookingDTO() {
    EventBookingDTO object = new EventBookingDTO();
    object.setBookingId(null);
    object.setUserBooked(new UserSummaryDTO());
    object.setUserBooked(null);
    object.setReservedById(null);
    object.setEventId(null);
    object.setEventTitle(null);
    object.setEventDate(null);
    object.setBookingDate(null);
    object.setUpdated(null);
    object.setBookingStatus(null);
    return object;
  }
}

