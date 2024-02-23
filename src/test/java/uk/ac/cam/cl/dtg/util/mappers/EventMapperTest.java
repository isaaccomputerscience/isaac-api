package uk.ac.cam.cl.dtg.util.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.cam.cl.dtg.util.mappers.MapperTestUtils.assertDeepEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBooking;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

class EventMapperTest {

  private final EventMapper eventMapper = EventMapper.INSTANCE;
  private static final Date testDate = new Date();
  private static final Date newTestDate = new Instant().plus(10000).toDate();

  @Test
  @DisplayName("Test mapping from DetailedEventBookingDTO to EventBookingDTO")
  void testEventMapping() throws JsonProcessingException {
    DetailedEventBookingDTO source = prepareDetailedEventBookingDTO();
    EventBookingDTO expected = prepareEventBookingDTO();

    EventBookingDTO result = eventMapper.map(source, EventBookingDTO.class);
    assertEquals(expected.getClass(), result.getClass());
    assertDeepEquals(expected, result);
  }

  @Test
  void unimplementedMappingExceptionForUnexpectedTarget() {
    DetailedEventBookingDTO source = new DetailedEventBookingDTO();
    Exception exception = assertThrows(UnimplementedMappingException.class, () -> eventMapper.map(source, DetailedEventBookingDTO.class));
    assertEquals("Invocation of unimplemented mapping from EventBookingDTO to DetailedEventBookingDTO", exception.getMessage());
  }

  private static EventBookingDTO prepareEventBookingDTO() {
    return new EventBookingDTO(
        3L,
        prepareUserSummaryDTO(),
        5L,
        "eventID",
        "eventTitle",
        testDate,
        testDate,
        newTestDate,
        BookingStatus.CONFIRMED);
  }

  private static DetailedEventBookingDTO prepareDetailedEventBookingDTO() {
    DetailedEventBookingDTO detailedEvent = new DetailedEventBookingDTO();
    detailedEvent.setBookingId(3L);
    detailedEvent.setUserBooked(prepareUserSummaryDTO());
    detailedEvent.setReservedById(5L);
    detailedEvent.setEventDate(testDate);
    detailedEvent.setEventId("eventID");
    detailedEvent.setEventTitle("eventTitle");
    detailedEvent.setBookingStatus(BookingStatus.CONFIRMED);
    detailedEvent.setBookingDate(testDate);
    detailedEvent.setUpdated(newTestDate);
    detailedEvent.setAdditionalInformation(prepareAdditionalInformation());
    return detailedEvent;
  }

  private static Map<String, String> prepareAdditionalInformation() {
    Map<String, String> additionalInformation = new HashMap<>();
    // Add additional information key-value pairs
    additionalInformation.put("key1", "value1");
    additionalInformation.put("key2", "value2");
    // Add more key-value pairs as needed
    return additionalInformation;
  }

  private static UserSummaryDTO prepareUserSummaryDTO() {
    return setUserSummaryDTOCommonFields(new UserSummaryDTO());
  }

  private static <T extends UserSummaryDTO> T setUserSummaryDTOCommonFields(T object) {
    UserContext userContext = new UserContext();
    userContext.setStage(Stage.a_level);
    userContext.setExamBoard(ExamBoard.aqa);

    object.setId(2L);
    object.setGivenName("givenName");
    object.setFamilyName("familyName");
    object.setRole(Role.TEACHER);
    object.setAuthorisedFullAccess(true);
    object.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
    object.setTeacherPending(true);
    object.setRegisteredContexts(List.of(userContext));
    return object;
  }
}

