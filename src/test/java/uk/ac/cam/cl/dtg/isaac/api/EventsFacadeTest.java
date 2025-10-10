package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueServerLogType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.DuplicateBookingException;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.EventIsFullException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.CompetitionEntryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.MainObjectMapper;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

class EventsFacadeCompetitionEntryTest {

  private EventsFacade eventsFacade;
  private PropertiesLoader properties;
  private ILogManager logManager;
  private EventBookingManager bookingManager;
  private UserAccountManager userManager;
  private GitContentManager contentManager;
  private UserBadgeManager userBadgeManager;
  private UserAssociationManager userAssociationManager;
  private GroupManager groupManager;
  private SchoolListReader schoolListReader;
  private MainObjectMapper mapper;
  private HttpServletRequest mockRequest;
  private String eventId;
  private RegisteredUserDTO mockTeacher;
  private RegisteredUserDTO mockStudent;
  private IsaacEventPageDTO mockEvent;

  @BeforeEach
  void beforeEach() {
    this.properties = createMock(PropertiesLoader.class);
    this.logManager = createMock(ILogManager.class);
    this.bookingManager = createMock(EventBookingManager.class);
    this.userManager = createMock(UserAccountManager.class);
    this.contentManager = createMock(GitContentManager.class);
    this.userBadgeManager = createMock(UserBadgeManager.class);
    this.userAssociationManager = createMock(UserAssociationManager.class);
    this.groupManager = createMock(GroupManager.class);
    this.schoolListReader = createMock(SchoolListReader.class);
    this.mapper = createMock(MainObjectMapper.class);
    this.eventsFacade = new EventsFacade(
        this.properties, this.logManager, this.bookingManager, this.userManager,
        this.contentManager, this.userBadgeManager, this.userAssociationManager,
        this.groupManager, this.schoolListReader, this.mapper
    );
    this.mockRequest = createMock(HttpServletRequest.class);
    this.eventId = "competition_event_123";

    this.mockTeacher = createMockTeacher();
    this.mockStudent = createMockStudent(1001L);
    this.mockEvent = createMockCompetitionEvent();
  }

  private RegisteredUserDTO createMockTeacher() {
    RegisteredUserDTO teacher = new RegisteredUserDTO();
    teacher.setId(5000L);
    teacher.setRole(Role.TEACHER);
    teacher.setGivenName("John");
    teacher.setFamilyName("Doe");
    teacher.setEmail("john.doe@school.com");
    teacher.setSchoolId("URN12345");
    teacher.setSchoolOther("Test School");
    return teacher;
  }

  private RegisteredUserDTO createMockStudent(Long id) {
    RegisteredUserDTO student = new RegisteredUserDTO();
    student.setId(id);
    student.setRole(Role.STUDENT);
    student.setGivenName("Jane");
    student.setFamilyName("Smith");
    student.setEmail("jane.smith@student.com");
    student.setSchoolId("URN12345");
    student.setSchoolOther("Test School");
    student.setGender(Gender.FEMALE);

    UserContext context = new UserContext();
    context.setStage(Stage.a_level);
    context.setExamBoard(ExamBoard.aqa);
    student.setRegisteredContexts(List.of(context));

    return student;
  }

  private IsaacEventPageDTO createMockCompetitionEvent() {
    IsaacEventPageDTO event = new IsaacEventPageDTO();
    event.setId(eventId);
    event.setTitle("Test Competition");
    event.setIsaacGroupToken("GROUP_TOKEN");
    event.setGroupReservationLimit(5);
    event.setPrivateEvent(false);
    event.setAllowGroupReservations(true);
    return event;
  }

  // Helper method to setup common expectations for getRawEventDTOById
  private void expectGetRawEventDTOById(String eventId, IsaacEventPageDTO event)
      throws ContentManagerException {
    expect(contentManager.getContentById(eventId)).andReturn(event);
    if (event != null) {
      expect(mapper.copy(event)).andReturn(event);
    }
  }

  @Test
  void createCompetitionEntry_noEventFound_returnsBadRequest()
      throws ContentManagerException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    expectGetRawEventDTOById(eventId, null);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_databaseException_returnsInternalServerError()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L))
        .andThrow(new SegueDatabaseException("Database error"));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_privateEvent_doesNotDeleteExistingBooking()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    IsaacEventPageDTO privateEvent = createMockCompetitionEvent();
    privateEvent.setPrivateEvent(true);

    EventBookingDTO booking = new EventBookingDTO();
    booking.setBookingStatus(BookingStatus.CONFIRMED);

    expectGetRawEventDTOById(eventId, privateEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(mockTeacher, mockStudent)).andReturn(true);
    expect(bookingManager.createCompetitionBooking(
        eq(privateEvent), eq(mockStudent), eq(mockTeacher),
        anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
        .andReturn(booking);

    logManager.logEvent(eq(mockTeacher), eq(mockRequest),
        eq(SegueServerLogType.EVENT_RESERVATIONS_CREATED),
        anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), eq(EventBookingDTO.class),
        eq(EventBookingDTO.class))).andReturn(List.of(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(bookingManager);
  }

  @Test
  void createCompetitionEntry_teacherRole_succeeds()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    EventBookingDTO booking = new EventBookingDTO();
    mockTeacher.setRole(Role.TEACHER);

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(mockTeacher, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        anyObject(), anyObject(), anyObject(), anyObject(Map.class), anyObject()))
        .andReturn(booking);

    logManager.logEvent(anyObject(), anyObject(), anyObject(), anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), anyObject(), anyObject()))
        .andReturn(Arrays.asList(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_eventLeaderRole_succeeds()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    EventBookingDTO booking = new EventBookingDTO();
    RegisteredUserDTO eventLeader = createMockTeacher();
    eventLeader.setRole(Role.EVENT_LEADER);

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(eventLeader);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(eventLeader, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        anyObject(), anyObject(), anyObject(), anyObject(Map.class), anyObject()))
        .andReturn(booking);

    logManager.logEvent(anyObject(), anyObject(), anyObject(), anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), anyObject(), anyObject()))
        .andReturn(Arrays.asList(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_eventManagerRole_succeeds()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    EventBookingDTO booking = new EventBookingDTO();
    RegisteredUserDTO eventManager = createMockTeacher();
    eventManager.setRole(Role.EVENT_MANAGER);

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(eventManager);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(eventManager, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        anyObject(), anyObject(), anyObject(), anyObject(Map.class), anyObject()))
        .andReturn(booking);

    logManager.logEvent(anyObject(), anyObject(), anyObject(), anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), anyObject(), anyObject()))
        .andReturn(List.of(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_adminRole_succeeds()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    EventBookingDTO booking = new EventBookingDTO();
    RegisteredUserDTO admin = createMockTeacher();
    admin.setRole(Role.ADMIN);

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(admin);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(admin, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        anyObject(), anyObject(), anyObject(), anyObject(Map.class), anyObject()))
        .andReturn(booking);

    logManager.logEvent(anyObject(), anyObject(), anyObject(), anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), anyObject(), anyObject()))
        .andReturn(List.of(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_withNullOptionalFields_succeeds()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));
    entryDTO.setSubmissionURL(null);
    entryDTO.setGroupName(null);
    entryDTO.setProjectTitle(null);

    EventBookingDTO booking = new EventBookingDTO();

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(mockTeacher, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        eq(mockEvent), eq(mockStudent), eq(mockTeacher),
        anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
        .andReturn(booking);

    logManager.logEvent(anyObject(), anyObject(), anyObject(), anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), anyObject(), anyObject()))
        .andReturn(List.of(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_studentWithoutContext_succeeds()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    RegisteredUserDTO studentNoContext = createMockStudent(1001L);
    studentNoContext.setRegisteredContexts(null);

    EventBookingDTO booking = new EventBookingDTO();

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(studentNoContext);
    expect(userAssociationManager.hasPermission(mockTeacher, studentNoContext)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        eq(mockEvent), eq(studentNoContext), eq(mockTeacher),
        anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
        .andReturn(booking);

    logManager.logEvent(anyObject(), anyObject(), anyObject(), anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), anyObject(), anyObject()))
        .andReturn(Arrays.asList(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_contentManagerException_returnsBadRequest()
      throws ContentManagerException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(Arrays.asList(1001L));

    expect(contentManager.getContentById(eventId)).andThrow(new ContentManagerException("Error"));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_eventDoesNotAllowGroupBookings_returnsForbidden()
      throws ContentManagerException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    IsaacEventPageDTO nonGroupEvent = createMockCompetitionEvent();
    nonGroupEvent.setIsaacGroupToken(null);
    nonGroupEvent.setAllowGroupReservations(false);

    expectGetRawEventDTOById(eventId, nonGroupEvent);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    assertEquals("This event does not accept group bookings.",
        response.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  void createCompetitionEntry_emptyEntrantList_returnsBadRequest()
      throws SegueDatabaseException, ContentManagerException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(new ArrayList<>());

    expectGetRawEventDTOById(eventId, mockEvent);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("At least one student must be selected for competition entry.",
        response.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  void createCompetitionEntry_nullEntrantList_returnsBadRequest()
      throws ContentManagerException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(null);

    expectGetRawEventDTOById(eventId, mockEvent);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("At least one student must be selected for competition entry.",
        response.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  void createCompetitionEntry_exceedsGroupLimit_returnsBadRequest()
      throws ContentManagerException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));

    IsaacEventPageDTO limitedEvent = createMockCompetitionEvent();
    limitedEvent.setGroupReservationLimit(5);

    expectGetRawEventDTOById(eventId, limitedEvent);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("Competition entries are limited to a maximum of 5 students.",
        response.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  void createCompetitionEntry_noGroupLimit_allowsAnyNumber()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(Arrays.asList(1001L, 1002L, 1003L, 1004L, 1005L, 1006L));

    IsaacEventPageDTO unlimitedEvent = createMockCompetitionEvent();
    unlimitedEvent.setGroupReservationLimit(null);

    expectGetRawEventDTOById(eventId, unlimitedEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);

    for (Long studentId : entryDTO.getEntrantIds()) {
      RegisteredUserDTO student = createMockStudent(studentId);
      EventBookingDTO booking = new EventBookingDTO();

      expect(userManager.getUserDTOById(studentId)).andReturn(student);
      expect(userAssociationManager.hasPermission(mockTeacher, student)).andReturn(true);
      expect(bookingManager.getBookingStatus(eventId, studentId)).andReturn(null);
      expect(bookingManager.createCompetitionBooking(
          eq(unlimitedEvent), eq(student), eq(mockTeacher),
          anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
          .andReturn(booking);
    }

    logManager.logEvent(eq(mockTeacher), eq(mockRequest),
        eq(SegueServerLogType.EVENT_RESERVATIONS_CREATED),
        anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), eq(EventBookingDTO.class),
        eq(EventBookingDTO.class))).andReturn(new ArrayList<>());

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_noUserLoggedIn_returnsNotLoggedIn()
      throws ContentManagerException, NoUserLoggedInException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest))
        .andThrow(new NoUserLoggedInException());

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_studentRole_returnsForbidden()
      throws ContentManagerException, NoUserLoggedInException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    RegisteredUserDTO studentUser = new RegisteredUserDTO();
    studentUser.setRole(Role.STUDENT);

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(studentUser);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_tutorRole_returnsForbidden()
      throws ContentManagerException, NoUserLoggedInException {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    RegisteredUserDTO tutorUser = new RegisteredUserDTO();
    tutorUser.setRole(Role.TUTOR);

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(tutorUser);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_noPermissionForUser_returnsForbidden()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(mockTeacher, mockStudent)).andReturn(false);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    assertEquals("You do not have permission to book or reserve some of these users onto this event.",
        response.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  void createCompetitionEntry_successfulSingleEntry_returnsOk()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));
    entryDTO.setSubmissionURL("https://github.com/team/project");
    entryDTO.setGroupName("Team Alpha");
    entryDTO.setProjectTitle("Amazing Project");

    EventBookingDTO booking = new EventBookingDTO();
    booking.setBookingStatus(BookingStatus.CONFIRMED);

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(mockTeacher, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        eq(mockEvent), eq(mockStudent), eq(mockTeacher),
        anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
        .andReturn(booking);

    logManager.logEvent(eq(mockTeacher), eq(mockRequest),
        eq(SegueServerLogType.EVENT_RESERVATIONS_CREATED),
        anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), eq(EventBookingDTO.class),
        eq(EventBookingDTO.class))).andReturn(List.of(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(logManager);
  }

  @Test
  void createCompetitionEntry_deletesExistingBookingForNonPrivateEvent_returnsOk()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    EventBookingDTO booking = new EventBookingDTO();
    booking.setBookingStatus(BookingStatus.CONFIRMED);

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(mockTeacher, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L))
        .andReturn(BookingStatus.WAITING_LIST);
    bookingManager.deleteBooking(mockEvent, mockStudent);
    expectLastCall();
    expect(bookingManager.createCompetitionBooking(
        eq(mockEvent), eq(mockStudent), eq(mockTeacher),
        anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
        .andReturn(booking);

    logManager.logEvent(eq(mockTeacher), eq(mockRequest),
        eq(SegueServerLogType.EVENT_RESERVATIONS_CREATED),
        anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(anyObject(List.class), eq(EventBookingDTO.class),
        eq(EventBookingDTO.class))).andReturn(List.of(booking));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(bookingManager);
  }

  @Test
  void createCompetitionEntry_multipleStudents_createsAllBookings()
      throws Exception {
    // Arrange
    List<Long> studentIds = Arrays.asList(1001L, 1002L, 1003L);
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(studentIds);
    entryDTO.setGroupName("Team Beta");

    List<EventBookingDTO> bookings = new ArrayList<>();

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);

    for (Long studentId : studentIds) {
      RegisteredUserDTO student = createMockStudent(studentId);
      EventBookingDTO booking = new EventBookingDTO();
      booking.setBookingStatus(BookingStatus.CONFIRMED);
      bookings.add(booking);

      expect(userManager.getUserDTOById(studentId)).andReturn(student);
      expect(userAssociationManager.hasPermission(mockTeacher, student)).andReturn(true);
      expect(bookingManager.getBookingStatus(eventId, studentId)).andReturn(null);
      expect(bookingManager.createCompetitionBooking(
          eq(mockEvent), eq(student), eq(mockTeacher),
          anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
          .andReturn(booking);
    }

    logManager.logEvent(eq(mockTeacher), eq(mockRequest),
        eq(SegueServerLogType.EVENT_RESERVATIONS_CREATED),
        anyObject(Map.class));
    expectLastCall();

    expect(mapper.mapList(eq(bookings), eq(EventBookingDTO.class),
        eq(EventBookingDTO.class))).andReturn(bookings);

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(bookingManager);
  }

  @Test
  void createCompetitionEntry_eventIsFull_returnsConflict()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(mockTeacher, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        eq(mockEvent), eq(mockStudent), eq(mockTeacher),
        anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
        .andThrow(new EventIsFullException("Event is full"));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
    assertEquals("There are not enough spaces available for this event. Please try again with fewer users.",
        response.readEntity(SegueErrorResponse.class).getErrorMessage());
  }

  @Test
  void createCompetitionEntry_duplicateBooking_returnsBadRequest()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andReturn(mockStudent);
    expect(userAssociationManager.hasPermission(mockTeacher, mockStudent)).andReturn(true);
    expect(bookingManager.getBookingStatus(eventId, 1001L)).andReturn(null);
    expect(bookingManager.createCompetitionBooking(
        eq(mockEvent), eq(mockStudent), eq(mockTeacher),
        anyObject(Map.class), eq(BookingStatus.CONFIRMED)))
        .andThrow(new DuplicateBookingException("Duplicate booking"));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void createCompetitionEntry_userNotFound_returnsNotFound()
      throws Exception {
    // Arrange
    CompetitionEntryDTO entryDTO = new CompetitionEntryDTO();
    entryDTO.setEntrantIds(List.of(1001L));

    expectGetRawEventDTOById(eventId, mockEvent);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockTeacher);
    expect(userManager.getUserDTOById(1001L)).andThrow(new NoUserException("User not found"));

    replay(properties, logManager, bookingManager, userManager, contentManager,
        userBadgeManager, userAssociationManager, groupManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.createCompetitionEntry(mockRequest, eventId, entryDTO);

    // Assert
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
}