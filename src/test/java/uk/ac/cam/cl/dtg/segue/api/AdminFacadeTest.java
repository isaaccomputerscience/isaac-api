package uk.ac.cam.cl.dtg.segue.api;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.segue.api.AbstractSegueFacade.isUserAnAdminOrEventManager;
import static uk.ac.cam.cl.dtg.util.ServletTestUtils.replayMockServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.IExternalAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

class AdminFacadeTest {
  private PropertiesLoader properties;
  private UserAccountManager userManager;
  private EmailManager emailManager;
  private GitContentManager contentManager;
  private String contentIndex;
  private ILogManager logManager;
  private StatisticsManager statsManager;
  private AbstractUserPreferenceManager userPreferenceManager;
  private EventBookingManager eventBookingManager;
  private SegueJobService segueJobService;
  private IExternalAccountManager externalAccountManager;
  private IMisuseMonitor misuseMonitor;
  private AdminFacade adminFacade;

  @BeforeEach
  final void beforeEach() {
    properties = createMock(PropertiesLoader.class);
    userManager = createMock(UserAccountManager.class);
    contentManager = createMock(GitContentManager.class);
    contentIndex = "";
    logManager = createMock(ILogManager.class);
    statsManager = createMock(StatisticsManager.class);
    userPreferenceManager = createMock(AbstractUserPreferenceManager.class);
    eventBookingManager = createMock(EventBookingManager.class);
    segueJobService = createMock(SegueJobService.class);
    externalAccountManager = createMock(IExternalAccountManager.class);
    misuseMonitor = createMock(IMisuseMonitor.class);
    emailManager = createMock(EmailManager.class);
    adminFacade = new AdminFacade(properties, userManager, contentManager, contentIndex, logManager, statsManager,
        userPreferenceManager, eventBookingManager, segueJobService, externalAccountManager, misuseMonitor, emailManager);
  }

  @Nested
  class ModifyUsersTeacherPendingStatus {
    @Nested
    class FailureResponses {
      @Test
      void modifyUsersTeacherPendingStatus_anonymousUser_returnsUnauthorised() throws NoUserLoggedInException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        List<Long> targetUsers = List.of(2L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andThrow(new NoUserLoggedInException());

        replay(userManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, false, targetUsers)) {
          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
          assertEquals("You must be logged in to access this resource.",
              response.readEntity(SegueErrorResponse.class).getErrorMessage());
        }

        verify(userManager);
      }

      @Test
      void modifyUsersTeacherPendingStatus_nonAdminUser_returnsForbidden() throws NoUserLoggedInException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of(2L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(isUserAnAdminOrEventManager(userManager, currentUser)).andReturn(false);

        replay(userManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, false, targetUsers)) {
          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
          assertEquals("You must be staff to access this endpoint.",
              response.readEntity(SegueErrorResponse.class).getErrorMessage());
        }

        verify(userManager);
      }

      @Test
      void modifyUsersTeacherPendingStatus_unknownTarget_returnsBadRequest()
          throws NoUserLoggedInException, SegueDatabaseException, NoUserException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of(2L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(isUserAnAdminOrEventManager(userManager, currentUser)).andReturn(true);

        expect(userManager.getUserDTOById(2L)).andThrow(new NoUserException("No user found with this ID!"));

        replay(userManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, false, targetUsers)) {
          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
          assertEquals("One or more users could not be found: [2]",
              response.readEntity(SegueErrorResponse.class).getErrorMessage());
        }

        verify(userManager);
      }

      @Test
      void modifyUsersTeacherPendingStatus_failureForOneOfMultipleTargets_returnsBadRequest()
          throws NoUserLoggedInException, SegueDatabaseException, NoUserException, ContentManagerException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of(2L, 3L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(isUserAnAdminOrEventManager(userManager, currentUser)).andReturn(true);

        expect(userManager.getUserDTOById(2L)).andThrow(new NoUserException("No user found with this ID!"));

        expect(userManager.getUserDTOById(3L)).andReturn(prepareTestUser(3L, true));
        expect(userManager.updateTeacherPendingFlag(3L, false)).andReturn(prepareTestUser(3L, false));

        expect(emailManager.getEmailTemplateDTO("teacher_declined")).andReturn(new EmailTemplateDTO());

        replay(userManager);
        replay(emailManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, false, targetUsers)) {
          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
          assertEquals("One or more users could not be found: [2]",
              response.readEntity(SegueErrorResponse.class).getErrorMessage());
        }

        verify(userManager);
      }

      @Test
      void modifyUsersTeacherPendingStatus_errorAccessingDatabase_returnsInternalServerError()
          throws NoUserLoggedInException, SegueDatabaseException, NoUserException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of(2L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(userManager.checkUserRole(currentUser, Arrays.asList(Role.ADMIN, Role.EVENT_MANAGER))).andReturn(true);

        expect(userManager.getUserDTOById(2L)).andThrow(new SegueDatabaseException("Postgres exception"));

        replay(userManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, false, targetUsers)) {
          assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
          assertEquals("Could not save new teacher_pending status to the database",
              response.readEntity(SegueErrorResponse.class).getErrorMessage());

        }

        verify(userManager);
      }
    }

    @Nested
    class SuccessResponses {

      @Test
      void modifyUsersTeacherPendingStatus_emptyTargetList_returnsOkOnSuccess() throws NoUserLoggedInException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of();

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(isUserAnAdminOrEventManager(userManager, currentUser)).andReturn(true);

        replay(userManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, false, targetUsers)) {
          assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        verify(userManager);
      }

      @Test
      void modifyUsersTeacherPendingStatus_updateOneTarget_returnsOkOnSuccess()
          throws NoUserLoggedInException, SegueDatabaseException, NoUserException, ContentManagerException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of(2L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(userManager.checkUserRole(currentUser, Arrays.asList(Role.ADMIN, Role.EVENT_MANAGER))).andReturn(true);

        expect(userManager.getUserDTOById(2L)).andReturn(prepareTestUser(2L, true));
        expect(userManager.updateTeacherPendingFlag(2L, false)).andReturn(prepareTestUser(2L, false));

        expect(emailManager.getEmailTemplateDTO("teacher_declined")).andReturn(new EmailTemplateDTO());
        replay(userManager);
        replay(userManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, false, targetUsers)) {
          assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        verify(userManager);
      }

      @Test
      void modifyUsersTeacherPendingStatus_updateMultipleTargets_returnsOkOnSuccess()
          throws NoUserLoggedInException, SegueDatabaseException, NoUserException, ContentManagerException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of(2L, 3L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(isUserAnAdminOrEventManager(userManager, currentUser)).andReturn(true);

        expect(userManager.getUserDTOById(2L)).andReturn(prepareTestUser(2L, true));
        expect(userManager.updateTeacherPendingFlag(2L, false)).andReturn(prepareTestUser(2L, false));

        expect(userManager.getUserDTOById(3L)).andReturn(prepareTestUser(3L, true));
        expect(userManager.updateTeacherPendingFlag(3L, false)).andReturn(prepareTestUser(3L, false));

        expect(emailManager.getEmailTemplateDTO("teacher_declined")).andReturn(new EmailTemplateDTO());

        replay(userManager);
        replay(emailManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, false, targetUsers)) {
          assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        verify(userManager);
      }

      @Test
      void modifyUsersTeacherPendingStatus_settingStatusTrue_returnsOkOnSuccess()
          throws NoUserLoggedInException, SegueDatabaseException, NoUserException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of(2L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(isUserAnAdminOrEventManager(userManager, currentUser)).andReturn(true);

        expect(userManager.getUserDTOById(2L)).andReturn(prepareTestUser(2L, false));
        expect(userManager.updateTeacherPendingFlag(2L, true)).andReturn(prepareTestUser(2L, true));

        replay(userManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, true, targetUsers)) {
          assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        verify(userManager);
      }

      @Test
      void modifyUsersTeacherPendingStatus_settingStatusToCurrentValue_returnsOkOnSuccess()
          throws NoUserLoggedInException, SegueDatabaseException, NoUserException {
        HttpServletRequest mockRequest = replayMockServletRequest();
        RegisteredUserDTO currentUser = new RegisteredUserDTO();
        List<Long> targetUsers = List.of(2L);

        expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(currentUser);
        expect(isUserAnAdminOrEventManager(userManager, currentUser)).andReturn(true);

        expect(userManager.getUserDTOById(2L)).andReturn(prepareTestUser(2L, true));
        expect(userManager.updateTeacherPendingFlag(2L, true)).andReturn(prepareTestUser(2L, true));

        replay(userManager);

        try (Response response = adminFacade.modifyUsersTeacherPendingStatus(mockRequest, true, targetUsers)) {
          assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        verify(userManager);
      }
    }

    RegisteredUserDTO prepareTestUser(Long id, boolean teacherPending) {
      RegisteredUserDTO testUser = new RegisteredUserDTO();
      testUser.setId(id);
      testUser.setTeacherPending(teacherPending);
      return testUser;
    }
  }
}