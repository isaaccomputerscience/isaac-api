package uk.ac.cam.cl.dtg.isaac.app;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.QuestionFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

public class QuestionFacadeTest {
  private PropertiesLoader dummyPropertiesLoader = null;
  private ContentMapper dummyMapper;
  private GitContentManager dummyContentManager;
  private GameManager dummyGameManager = null;
  private String contentIndex;
  private UserAccountManager userManager;
  private QuestionManager dummyQuestionManager;

  private ILogManager dummyLogManager = null;
  private IMisuseMonitor misuseMonitor;
  private UserBadgeManager userBadgeManager;
  private IUserStreaksManager userStreaksManager;
  private UserAssociationManager userAssociationManager;

  /**
   * Initial configuration of tests.
   *
   * @throws Exception - test exception
   */

  @Before
  public void setUp() throws Exception {
    this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
    this.dummyMapper = createMock(ContentMapper.class);
    this.dummyContentManager = createMock(GitContentManager.class);
    this.dummyGameManager = createMock(GameManager.class);
    this.contentIndex = "Test";
    this.userManager = createMock(UserAccountManager.class);
    this.dummyQuestionManager = createMock(QuestionManager.class);
    this.dummyLogManager = createMock(ILogManager.class);
    this.misuseMonitor = createMock(IMisuseMonitor.class);
    this.userBadgeManager = createMock(UserBadgeManager.class);
    this.userStreaksManager = createMock(IUserStreaksManager.class);
    this.userAssociationManager = createMock(UserAssociationManager.class);
  }

  /**
   * Verify that when a user is logged in and the questions are generated successfully
   *
   * @return
   */

  @Test
  public void getRandomQuestions_UserLoggedInAndQuestionsGenerated_ReturnsOk()
      throws NoUserLoggedInException, ContentManagerException {

    // Arrange
    QuestionFacade questionFacade =
        new QuestionFacade(dummyPropertiesLoader, dummyMapper, dummyContentManager, dummyGameManager,
            contentIndex, userManager, dummyQuestionManager, dummyLogManager, misuseMonitor,
            userBadgeManager, userStreaksManager, userAssociationManager);

    HttpServletRequest dummyRequest = createMock(HttpServletRequest.class);;
    String subjects = "compSci";
    RegisteredUserDTO user = new RegisteredUserDTO();
    List<IsaacQuestionPageDTO> questions = new ArrayList<>();
    replay(dummyRequest);

    expect(userManager.getCurrentRegisteredUser(dummyRequest)).andReturn(new RegisteredUserDTO()).atLeastOnce();
    expect(dummyGameManager.generateRandomQuestions(EasyMock.anyObject(), EasyMock.anyInt())).andReturn(questions).atLeastOnce();
    replay(userManager, dummyGameManager);


      // Act
      Response res = questionFacade.getRandomQuestions(dummyRequest, subjects);

      // Assert
      assertEquals(res.getStatus(), Status.OK.getStatusCode());
      verify(userManager, dummyGameManager);
  }

  @Test
  public void getRandomQuestionsTest() {
    QuestionFacade questionFacade =
        new QuestionFacade(dummyPropertiesLoader, dummyMapper, dummyContentManager, dummyGameManager,
            contentIndex, userManager, dummyQuestionManager, dummyLogManager, misuseMonitor,
            userBadgeManager, userStreaksManager, userAssociationManager);
    HttpServletRequest mockRequest = createMock(HttpServletRequest.class);
    String subjects = "compSci";

    try (Response response = questionFacade.getRandomQuestions(mockRequest, subjects)) {
      assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
  }
}
