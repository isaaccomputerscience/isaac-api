package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createMock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserStreakManager;
import uk.ac.cam.cl.dtg.segue.api.QuestionFacade;

public class QuestionFacadeIT extends IsaacIntegrationTest {

  private QuestionFacade questionFacade;
  private static final String contentIndex = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

  @BeforeEach
  public void beforeEach() {
    IUserStreaksManager userStreaksManager = createMock(PgUserStreakManager.class);
    questionFacade =
        new QuestionFacade(properties, contentMapper, contentManager, contentIndex, userAccountManager, questionManager,
            logManager, misuseMonitor, userBadgeManager, userStreaksManager, userAssociationManager);
  }

  @Test
  public void answerQuestionNotAvailableForQuizQuestions() {
    HttpServletRequest mockRequest = createMock(HttpServletRequest.class);
    String questionId =
        "_hidden_from_roles_tutor_quiz_test|0ec982f6-e2bf-4974-b777-c50b9471beb1|84c48a78-2a27-4843-866a-c8895aa60e70";
    String jsonAnswer = "42";

    Response response = questionFacade.answerQuestion(mockRequest, questionId, jsonAnswer);

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }
}
