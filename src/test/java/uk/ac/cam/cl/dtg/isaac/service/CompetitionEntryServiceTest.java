package uk.ac.cam.cl.dtg.isaac.service;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.services.CompetitionEntryService;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.CompetitionEntryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;

class CompetitionEntryServiceTest {

  private CompetitionEntryService competitionEntryService;
  private EmailManager emailManager;
  private UserAccountManager userAccountManager;
  private IsaacEventPageDTO mockEvent;
  private CompetitionEntryDTO mockEntryDTO;
  private RegisteredUserDTO mockTeacher;
  private EmailTemplateDTO mockEmailTemplate;

  @BeforeEach
  void beforeEach() {
    this.emailManager = createMock(EmailManager.class);
    this.userAccountManager = createMock(UserAccountManager.class);
    this.competitionEntryService = new CompetitionEntryService(
        this.emailManager,
        this.userAccountManager
    );

    this.mockEvent = createMockEvent();
    this.mockEntryDTO = createMockEntryDTO();
    this.mockTeacher = createMockTeacher();
    this.mockEmailTemplate = createMock(EmailTemplateDTO.class);
  }

  private IsaacEventPageDTO createMockEvent() {
    IsaacEventPageDTO event = new IsaacEventPageDTO();
    event.setId("competition_event_123");
    event.setTitle("Test Competition 2025");
    return event;
  }

  private CompetitionEntryDTO createMockEntryDTO() {
    CompetitionEntryDTO dto = new CompetitionEntryDTO();
    dto.setEntrantIds(Arrays.asList(1001L, 1002L, 1003L));
    dto.setProjectTitle("Amazing AI Project");
    dto.setSubmissionURL("https://github.com/team/project");
    dto.setGroupName("Team Alpha");
    return dto;
  }

  private RegisteredUserDTO createMockTeacher() {
    RegisteredUserDTO teacher = new RegisteredUserDTO();
    teacher.setId(5000L);
    teacher.setGivenName("John");
    teacher.setFamilyName("Doe");
    teacher.setEmail("john.doe@school.com");
    teacher.setRole(Role.TEACHER);
    return teacher;
  }

  private RegisteredUserDTO createMockStudent(Long id, String givenName, String familyName) {
    RegisteredUserDTO student = new RegisteredUserDTO();
    student.setId(id);
    student.setGivenName(givenName);
    student.setFamilyName(familyName);
    student.setEmail(givenName.toLowerCase() + "@student.com");
    student.setRole(Role.STUDENT);
    return student;
  }

  @Test
  void sendCompetitionEntryConfirmation_validInputs_sendsEmailSuccessfully()
      throws Exception {
    // Arrange
    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student2 = createMockStudent(1002L, "Bob", "Smith");
    RegisteredUserDTO student3 = createMockStudent(1003L, "Charlie", "Brown");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L)).andReturn(student2);
    expect(userAccountManager.getUserDTOById(1003L)).andReturn(student3);

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        anyObject(Map.class),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
  }

  @Test
  void sendCompetitionEntryConfirmation_nullEvent_logsErrorAndDoesNotSendEmail() {
    // Arrange
    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        null,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    // Verify no interactions occurred
  }

  @Test
  void sendCompetitionEntryConfirmation_nullEntryDTO_logsErrorAndDoesNotSendEmail() {
    // Arrange
    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        null,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    // Verify no interactions occurred
  }

  @Test
  void sendCompetitionEntryConfirmation_nullReservingUser_logsErrorAndDoesNotSendEmail() {
    // Arrange
    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        null
    );

    // Assert
    verify(emailManager, userAccountManager);
    // Verify no interactions occurred
  }

  @Test
  void sendCompetitionEntryConfirmation_withNullProjectTitle_sendsEmailWithEmptyString()
      throws Exception {
    // Arrange
    mockEntryDTO.setProjectTitle(null);

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student2 = createMockStudent(1002L, "Bob", "Smith");
    RegisteredUserDTO student3 = createMockStudent(1003L, "Charlie", "Brown");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L)).andReturn(student2);
    expect(userAccountManager.getUserDTOById(1003L)).andReturn(student3);

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        anyObject(Map.class),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
  }

  @Test
  void sendCompetitionEntryConfirmation_withNullSubmissionURL_sendsEmailWithEmptyString()
      throws Exception {
    // Arrange
    mockEntryDTO.setSubmissionURL(null);

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student2 = createMockStudent(1002L, "Bob", "Smith");
    RegisteredUserDTO student3 = createMockStudent(1003L, "Charlie", "Brown");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L)).andReturn(student2);
    expect(userAccountManager.getUserDTOById(1003L)).andReturn(student3);

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        anyObject(Map.class),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
  }

  @Test
  void sendCompetitionEntryConfirmation_withNullGroupName_sendsEmailWithEmptyString()
      throws Exception {
    // Arrange
    mockEntryDTO.setGroupName(null);

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student2 = createMockStudent(1002L, "Bob", "Smith");
    RegisteredUserDTO student3 = createMockStudent(1003L, "Charlie", "Brown");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L)).andReturn(student2);
    expect(userAccountManager.getUserDTOById(1003L)).andReturn(student3);

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        anyObject(Map.class),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
  }

  @Test
  void sendCompetitionEntryConfirmation_withAllNullOptionalFields_sendsEmailSuccessfully()
      throws Exception {
    // Arrange
    mockEntryDTO.setProjectTitle(null);
    mockEntryDTO.setSubmissionURL(null);
    mockEntryDTO.setGroupName(null);

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student2 = createMockStudent(1002L, "Bob", "Smith");
    RegisteredUserDTO student3 = createMockStudent(1003L, "Charlie", "Brown");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L)).andReturn(student2);
    expect(userAccountManager.getUserDTOById(1003L)).andReturn(student3);

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        anyObject(Map.class),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
  }

  @Test
  void sendCompetitionEntryConfirmation_singleStudent_sendsEmailSuccessfully()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L));

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        anyObject(Map.class),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
  }


  void formatStudentsList_nullEntrantIds_returnsNoStudentsListed()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(null);

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    assertEquals("No students listed", capturedContext.get("studentsList"));
  }


  void formatStudentsList_emptyEntrantIds_returnsNoStudentsListed()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Collections.emptyList());

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    assertEquals("No students listed", capturedContext.get("studentsList"));
  }


  void formatStudentsList_studentNotFound_includesPlaceholderInList()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L, 1002L));

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L))
        .andThrow(new NoUserException("User not found"));

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");
    assertTrue(studentsList.contains("<ul>"));
    assertTrue(studentsList.contains("</ul>"));
    assertTrue(studentsList.contains("<li>Alice Johnson</li>"));
    assertTrue(studentsList.contains("<li>Student ID 1002 (user not found)</li>"));
  }


  void formatStudentsList_allStudentsNotFound_sendsEmailWithPlaceholders()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L, 1002L));

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L))
        .andThrow(new NoUserException("User not found"));
    expect(userAccountManager.getUserDTOById(1002L))
        .andThrow(new NoUserException("User not found"));

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");
    assertTrue(studentsList.contains("<ul>"));
    assertTrue(studentsList.contains("</ul>"));
    assertTrue(studentsList.contains("<li>Student ID 1001 (user not found)</li>"));
    assertTrue(studentsList.contains("<li>Student ID 1002 (user not found)</li>"));
  }


  void formatStudentsList_studentWithNullGivenName_handlesGracefully()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L));

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    student1.setGivenName(null);

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");
    assertTrue(studentsList.contains("<li>Johnson</li>"));
  }


  void formatStudentsList_studentWithNullFamilyName_handlesGracefully()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L));

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    student1.setFamilyName(null);

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");
    assertTrue(studentsList.contains("<li>Alice</li>"));
  }


  void formatStudentsList_studentWithBothNullNames_handlesGracefully()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L));

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    student1.setGivenName(null);
    student1.setFamilyName(null);

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");
    assertTrue(studentsList.contains("<li>Unknown name</li>"));
  }


  void formatStudentsList_unexpectedExceptionDuringLookup_logsErrorAndContinues()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L, 1002L));

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L))
        .andThrow(new RuntimeException("Unexpected error"));

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");
    assertTrue(studentsList.contains("<li>Alice Johnson</li>"));
    // Student 1002 should be skipped due to exception
  }


  void formatStudentsList_allExceptionsSkipped_returnsNoStudentsListed()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L, 1002L));

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L))
        .andThrow(new RuntimeException("Unexpected error"));
    expect(userAccountManager.getUserDTOById(1002L))
        .andThrow(new RuntimeException("Unexpected error"));

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");
    assertEquals("No students listed", studentsList);
  }

  @Test
  void sendCompetitionEntryConfirmation_manyStudents_sendsEmailSuccessfully()
      throws Exception {
    // Arrange
    List<Long> manyStudentIds = Arrays.asList(1001L, 1002L, 1003L, 1004L, 1005L);
    mockEntryDTO.setEntrantIds(manyStudentIds);

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    for (int i = 0; i < manyStudentIds.size(); i++) {
      Long studentId = manyStudentIds.get(i);
      RegisteredUserDTO student = createMockStudent(
          studentId,
          "Student" + i,
          "LastName" + i
      );
      expect(userAccountManager.getUserDTOById(studentId)).andReturn(student);
    }

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        anyObject(Map.class),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
  }


  void sendCompetitionEntryConfirmation_mixedSuccessAndFailureLookups_sendsEmailWithAvailableData()
      throws Exception {
    // Arrange
    mockEntryDTO.setEntrantIds(Arrays.asList(1001L, 1002L, 1003L));

    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student3 = createMockStudent(1003L, "Charlie", "Brown");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L))
        .andThrow(new NoUserException("User not found"));
    expect(userAccountManager.getUserDTOById(1003L)).andReturn(student3);

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");
    assertTrue(studentsList.contains("<li>Alice Johnson</li>"));
    assertTrue(studentsList.contains("<li>Student ID 1002 (user not found)</li>"));
    assertTrue(studentsList.contains("<li>Charlie Brown</li>"));
  }

  @Test
  void sendCompetitionEntryConfirmation_emailManagerThrowsExceptionDuringSend_logsErrorAndContinues()
      throws Exception {
    // Arrange
    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student2 = createMockStudent(1002L, "Bob", "Smith");
    RegisteredUserDTO student3 = createMockStudent(1003L, "Charlie", "Brown");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L)).andReturn(student2);
    expect(userAccountManager.getUserDTOById(1003L)).andReturn(student3);

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        anyObject(Map.class),
        eq(EmailType.SYSTEM)
    );
    expectLastCall().andThrow(new RuntimeException("Email send failed"));

    replay(emailManager, userAccountManager);

    // Act - should not throw exception
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);
  }


  void sendCompetitionEntryConfirmation_verifyEmailContextContainsAllRequiredFields()
      throws Exception {
    // Arrange
    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student2 = createMockStudent(1002L, "Bob", "Smith");
    RegisteredUserDTO student3 = createMockStudent(1003L, "Charlie", "Brown");

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L)).andReturn(student2);
    expect(userAccountManager.getUserDTOById(1003L)).andReturn(student3);

    // Capture the email context to verify its contents
    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);

    Map<String, Object> capturedContext = emailContextCapture.getValue();
    assertNotNull(capturedContext);
    assertTrue(capturedContext.containsKey("event"));
    assertTrue(capturedContext.containsKey("givenName"));
    assertTrue(capturedContext.containsKey("projectTitle"));
    assertTrue(capturedContext.containsKey("projectLink"));
    assertTrue(capturedContext.containsKey("groupName"));
    assertTrue(capturedContext.containsKey("studentsList"));
    assertTrue(capturedContext.containsKey("contactUsURL"));

    assertEquals(mockEvent, capturedContext.get("event"));
    assertEquals("John", capturedContext.get("givenName"));
    assertEquals("Amazing AI Project", capturedContext.get("projectTitle"));
    assertEquals("https://github.com/team/project", capturedContext.get("projectLink"));
    assertEquals("Team Alpha", capturedContext.get("groupName"));
    assertEquals("contact@isaaccomputerscience.org", capturedContext.get("contactUsURL"));

    String studentsList = (String) capturedContext.get("studentsList");
    assertTrue(studentsList.contains("<ul>"));
    assertTrue(studentsList.contains("</ul>"));
    assertTrue(studentsList.contains("<li>Alice Johnson</li>"));
    assertTrue(studentsList.contains("<li>Bob Smith</li>"));
    assertTrue(studentsList.contains("<li>Charlie Brown</li>"));
  }


  void formatStudentsList_generatesValidHtmlBulletList()
      throws Exception {
    // Arrange
    RegisteredUserDTO student1 = createMockStudent(1001L, "Alice", "Johnson");
    RegisteredUserDTO student2 = createMockStudent(1002L, "Bob", "Smith");

    mockEntryDTO.setEntrantIds(Arrays.asList(1001L, 1002L));

    expect(emailManager.getEmailTemplateDTO("email_competition_entry_confirmation"))
        .andReturn(mockEmailTemplate);

    expect(userAccountManager.getUserDTOById(1001L)).andReturn(student1);
    expect(userAccountManager.getUserDTOById(1002L)).andReturn(student2);

    Capture<Map<String, Object>> emailContextCapture = newCapture();

    emailManager.sendTemplatedEmailToUser(
        eq(mockTeacher),
        eq(mockEmailTemplate),
        capture(emailContextCapture),
        eq(EmailType.SYSTEM)
    );
    expectLastCall();

    replay(emailManager, userAccountManager);

    // Act
    competitionEntryService.sendCompetitionEntryConfirmation(
        mockEvent,
        mockEntryDTO,
        mockTeacher
    );

    // Assert
    verify(emailManager, userAccountManager);

    Map<String, Object> capturedContext = emailContextCapture.getValue();
    String studentsList = (String) capturedContext.get("studentsList");

    // Verify HTML structure
    assertTrue(studentsList.startsWith("<ul>"));
    assertTrue(studentsList.endsWith("</ul>"));
    assertTrue(studentsList.contains("<li>Alice Johnson</li>"));
    assertTrue(studentsList.contains("<li>Bob Smith</li>"));

    // Verify no plain text newlines
    assertFalse(studentsList.contains("\n- "));
  }
}