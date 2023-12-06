/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.HIDE_FROM_FILTER_TAG;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.RELATED_CONTENT_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEPRECATED_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DIFFICULTY_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EXAM_BOARD_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LEVEL_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.STAGE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueServerLogType;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager.extractPageIdFromQuestionId;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.TestCase;
import uk.ac.cam.cl.dtg.isaac.dos.TestQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatorUnavailableException;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.AnonQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.IPQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.QuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.LogUtils;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.RequestIpExtractor;

/**
 * Question Facade
 * <br>
 * This facade is intended to support external interaction with segue supported questions.
 */
@Path("/questions")
@Tag(name = "/questions")
public class QuestionFacade extends AbstractSegueFacade {
  private static final Logger log = LoggerFactory.getLogger(QuestionFacade.class);
  private final ContentMapper mapper;
  private final GameManager gameManager;
  private final GitContentManager contentManager;
  private final String contentIndex;
  private final UserAccountManager userManager;
  private final QuestionManager questionManager;
  private final UserBadgeManager userBadgeManager;
  private final UserAssociationManager userAssociationManager;
  private IMisuseMonitor misuseMonitor;
  private IUserStreaksManager userStreaksManager;

  /**
   * @param properties             - the fully configured properties loader for the api.
   * @param mapper                 - The Content mapper object used for polymorphic mapping of content objects.
   * @param contentManager         - The content version controller used by the api.
   * @param gameManager            - The manager object responsible for games.
   * @param contentIndex           - The index string for current content version
   * @param userManager            - The manager object responsible for users.
   * @param questionManager        - A question manager object responsible for managing questions and augmenting
   *                               questions with user information.
   * @param logManager             - An instance of the log manager used for recording usage of the CMS.
   * @param misuseMonitor          - An instance of the misuse monitor for rate limiting answer attempts
   * @param userBadgeManager       - An instance of the badge manager
   * @param userStreaksManager     - An instance of the streaks manager to notify users when their answer streak changes
   * @param userAssociationManager - An instance of the association manager to check for teacher permissions over other
   *                               users
   */
  @Inject
  public QuestionFacade(final PropertiesLoader properties, final ContentMapper mapper,
                        final GitContentManager contentManager, final GameManager gameManager,
                        @Named(CONTENT_INDEX) final String contentIndex,
                        final UserAccountManager userManager, final QuestionManager questionManager,
                        final ILogManager logManager, final IMisuseMonitor misuseMonitor,
                        final UserBadgeManager userBadgeManager, final IUserStreaksManager userStreaksManager,
                        final UserAssociationManager userAssociationManager) {
    super(properties, logManager);

    this.questionManager = questionManager;
    this.mapper = mapper;
    this.contentManager = contentManager;
    this.gameManager = gameManager;
    this.contentIndex = contentIndex;
    this.userManager = userManager;
    this.misuseMonitor = misuseMonitor;
    this.userStreaksManager = userStreaksManager;
    this.userBadgeManager = userBadgeManager;
    this.userAssociationManager = userAssociationManager;
  }

  /**
   * Warn users attempting to make GET requests to answers that we do not provide these. Log the attempt.
   *
   * @param request    - the incoming request
   * @param questionId - the question the user is referring to
   * @return an error message informing the user where to find help.
   */
  @GET
  @Path("{question_id}/answer")
  @Operation(summary = "Provide users who try to cheat with a guide to the location of our help page.")
  public Response getQuestionAnswer(@Context final HttpServletRequest request,
                                    @PathParam("question_id") final String questionId) {
    String errorMessage =
        String.format("We do not provide answers to questions. See https://%s/solving_problems for more help!",
            getProperties().getProperty(HOST_NAME));
    try {
      AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(request);
      if (currentUser instanceof RegisteredUserDTO) {
        log.warn(String.format("MethodNotAllowed: User (%s) attempted to GET the answer to the question '%s'!",
            ((RegisteredUserDTO) currentUser).getId(), sanitiseExternalLogValue(questionId)));
      } else {
        log.warn(String.format("MethodNotAllowed: Anonymous user attempted to GET the answer to the question '%s'!",
            sanitiseExternalLogValue(questionId)));
      }
      return new SegueErrorResponse(Status.METHOD_NOT_ALLOWED, errorMessage).toResponse();
    } catch (SegueDatabaseException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Database error while looking up user information.", e);
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    }
  }

  /**
   * Get questions answered by user per month for a given date range.
   *
   * @param request          - the incoming request
   * @param userIdOfInterest - The user id that the query is focused on
   * @param fromDate         - the date to start counting (and the month that will be first in the response map)
   * @param toDate           - The date to finish counting and the month that will be last in the response map
   * @param perDay           - Whether to bin by day, instead of by month as default.
   * @return an object containing dates (first of each month) mapped to number (number of question attempts)
   */
  @GET
  @Path("answered_questions/{user_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Return a count of question attempts per month.")
  public Response getQuestionsAnswered(@Context final HttpServletRequest request,
                                       @PathParam("user_id") final Long userIdOfInterest,
                                       @QueryParam("from_date") final Long fromDate,
                                       @QueryParam("to_date") final Long toDate,
                                       @QueryParam("per_day") final Boolean perDay) {
    try {

      if (null == fromDate || null == toDate) {
        return new SegueErrorResponse(Status.BAD_REQUEST,
            "You must specify the from_date and to_date you are interested in.").toResponse();
      }

      if (fromDate > toDate) {
        return new SegueErrorResponse(Status.BAD_REQUEST,
            "The from_date must be before the to_date!").toResponse();
      }

      RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);

      RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(userIdOfInterest);
      UserSummaryDTO userOfInterestSummaryObject = userManager.convertToUserSummaryObject(userOfInterest);

      // decide if the user is allowed to view this data. If user isn't viewing their own data, user viewing
      // must have a valid connection with the user of interest and be at least a teacher.
      if (!currentUser.getId().equals(userIdOfInterest)
          && !userAssociationManager.hasTeacherPermission(currentUser, userOfInterestSummaryObject)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      // No point looking for stats from before the user registered (except for merged attempts at registration,
      // and these will only be ANONYMOUS_SESSION_DURATION_IN_MINUTES before registration anyway):
      Date fromDateObject = new Date(fromDate);
      if (fromDateObject.before(userOfInterest.getRegistrationDate())) {
        fromDateObject = userOfInterest.getRegistrationDate();
      }

      return Response.ok(
              this.questionManager.getUsersQuestionAttemptCountsByDate(userOfInterest, fromDateObject, new Date(toDate),
                  perDay))
          .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (NoUserException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to find user with the id provided.").toResponse();
    } catch (SegueDatabaseException e) {
      log.error("Unable to look up user event history for user " + userIdOfInterest, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while looking up event information")
          .toResponse();
    }
  }

  /**
   * REST end point to provide five questions in random located in the question tile of the student dashboard.
   *
   * @param request    - this allows us to check to see if a user is currently loggedin.
   * @param subjects   - a comma separated list of subjects
   * @param stages     - a comma separated list of stages
   * @param examBoards - a comma separated list of examBoards
   * @return a Response containing a gameboard object or containing a SegueErrorResponse.
   */
  @GET
  @Path("/randomQuestions")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  public final Response getRandomQuestions(@Context final HttpServletRequest request,
                                           @QueryParam("subjects") final String subjects,
                                           @QueryParam("stages") final String stages,
                                           @QueryParam("examBoards") final String examBoards)
      throws ContentManagerException {

    List<String> subjectsList = splitCsvStringQueryParam(subjects);
    List<String> stagesList = splitCsvStringQueryParam(stages);
    List<String> examBoardsList = splitCsvStringQueryParam(examBoards);

    GameFilter gameFilter = new GameFilter(
        subjectsList,
        null,
        null,
        null,
        null,
        null,
        stagesList,
        null,
        examBoardsList);

    var questions = this.gameManager.generateRandomQuestions(gameFilter, 5);

    // Return the list of random questions as JSON
    return Response.ok(questions).build();
  }


  /**
   * Record that a user has answered a question.
   *
   * @param request    - the servlet request so we can find out if it is a known user.
   * @param questionId that you are attempting to answer.
   * @param jsonAnswer - answer body which will be parsed as a Choice and then converted to a ChoiceDTO.
   * @return Response containing a QuestionValidationResponse object or containing a SegueErrorResponse .
   */
  @POST
  @Path("{question_id}/answer")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Submit an answer to a question.",
      description = "The answer must be the correct Choice subclass for the question with the provided ID.")
  public Response answerQuestion(@Context final HttpServletRequest request,
                                 @PathParam("question_id") final String questionId, final String jsonAnswer) {
    if (null == jsonAnswer || jsonAnswer.isEmpty()) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
    }

    Content contentBasedOnId;
    try {
      contentBasedOnId = this.contentManager.getContentDOById(
          questionId);
    } catch (ContentManagerException e1) {
      SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
          e1);
      log.error(error.getErrorMessage(), e1);
      return error.toResponse();
    }

    Question question;
    if (contentBasedOnId instanceof Question) {
      question = (Question) contentBasedOnId;
    } else {
      SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
          "No question object found for given id: " + sanitiseExternalLogValue(questionId));
      log.warn(error.getErrorMessage());
      return error.toResponse();
    }

    // Prevent attempting a question through this endpoint if this question is part of a quiz.
    String questionPageId = extractPageIdFromQuestionId(questionId);
    Content pageContent;
    try {
      pageContent = this.contentManager.getContentDOById(questionPageId);
      if (pageContent instanceof IsaacQuiz) {
        return new SegueErrorResponse(Status.FORBIDDEN, "This question is part of a quiz").toResponse();
      }
    } catch (ContentManagerException e) {
      // This doesn't make sense, so we'll log and continue.
      SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Question without page found", e);
      log.error(error.getErrorMessage(), e);
    }

    try {
      ChoiceDTO answerFromClientDTO = questionManager.convertJsonAnswerToChoice(jsonAnswer);

      AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(request);

      Response response = this.questionManager.validateAnswer(question, answerFromClientDTO);

      // After validating the answer, work out whether this is abuse of the endpoint. If so, record the attempt in
      // the log, but don't save it for the user. Also, return an error.

      // We store response.getEntity() in either case so that we can treat them the same in later analysis.
      if (currentUser instanceof RegisteredUserDTO) {
        try {
          // Monitor misuse on a per-question per-registered user basis, with higher limits:
          misuseMonitor.notifyEvent(((RegisteredUserDTO) currentUser).getId().toString() + "|" + questionId,
              QuestionAttemptMisuseHandler.class.getSimpleName());
        } catch (SegueResourceMisuseException e) {
          this.getLogManager()
              .logEvent(currentUser, request, SegueServerLogType.QUESTION_ATTEMPT_RATE_LIMITED, response.getEntity());
          String message = "You have made too many attempts at this question part. Please try again later.";
          return SegueErrorResponse.getRateThrottledResponse(message);
        }
      } else {
        try {
          // Monitor misuse on a per-question per-anonymous user basis:
          misuseMonitor.notifyEvent(((AnonymousUserDTO) currentUser).getSessionId() + "|" + questionId,
              AnonQuestionAttemptMisuseHandler.class.getSimpleName());
        } catch (SegueResourceMisuseException e) {
          this.getLogManager()
              .logEvent(currentUser, request, SegueServerLogType.QUESTION_ATTEMPT_RATE_LIMITED, response.getEntity());
          String message = "You have made too many attempts at this question part. Please log in or try again later.";
          return SegueErrorResponse.getRateThrottledResponse(message);
        }
        try {
          // And monitor on a blanket per IP Address basis for non-logged in users.
          // If we see serious misuse, this could be moved to *before* the attempt validation and checking,
          // to save server load. Since this occurs after the anon user notify event, that will catch most
          // misuse and this will catch misuse ignoring cookies or with repeated new anon accounts.
          misuseMonitor.notifyEvent(RequestIpExtractor.getClientIpAddr(request),
              IPQuestionAttemptMisuseHandler.class.getSimpleName());
        } catch (SegueResourceMisuseException e) {
          this.getLogManager()
              .logEvent(currentUser, request, SegueServerLogType.QUESTION_ATTEMPT_RATE_LIMITED, response.getEntity());
          String message = "Too many question attempts! Please either create an account, log in, or try again later.";
          return SegueErrorResponse.getRateThrottledResponse(message);
        }
      }

      // If we get to this point, this is a valid question attempt. Record it.
      if (response.getEntity() instanceof QuestionValidationResponseDTO) {
        questionManager.recordQuestionAttempt(currentUser,
            (QuestionValidationResponseDTO) response.getEntity());
      }

      this.getLogManager().logEvent(currentUser, request, SegueServerLogType.ANSWER_QUESTION, response.getEntity());

      // Update the user in case their streak has changed:
      if (currentUser instanceof RegisteredUserDTO) {
        this.userStreaksManager.notifyUserOfStreakChange((RegisteredUserDTO) currentUser);
      }

      return response;

    } catch (IllegalArgumentException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Bad request - " + e.getMessage(), e);
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    } catch (SegueDatabaseException e) {
      SegueErrorResponse error =
          new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to save question attempt. Try again later!");
      log.error("Unable to to record question attempt.", e);
      return error.toResponse();
    } catch (ErrorResponseWrapper responseWrapper) {
      return responseWrapper.toResponse();
    }
  }

  /**
   * A generic question tester where a fake question is created form received choices and evaluated against a series.
   * of example student answers
   *
   * @param request      - the incoming request
   * @param questionType - the type of question to construct from the available choices in testJson
   * @param testJson     - a JSON structure to represent the possible choices and
   * @return a list of test cases matching those that were sent to the endpoint augmented with the validator's results
   */
  @POST
  @Path("/test")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Test a list of choices with some expected answer values")
  public Response testQuestion(@Context final HttpServletRequest request,
                               @QueryParam("type") final String questionType, final String testJson) {
    try {
      RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
      if (!isUserStaff(userManager, currentUser)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      TestQuestion testDefinition = mapper.getSharedContentObjectMapper().readValue(testJson, TestQuestion.class);
      List<TestCase> results = questionManager.testQuestion(questionType, testDefinition);
      return Response.ok(results).build();

    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (ValidatorUnavailableException | IOException e) {
      return SegueErrorResponse.getServiceUnavailableResponse(e.getMessage());
    }
  }

  /**
   * Convert a possible answer into a question specification.
   *
   * @param request    - the servlet request so we can find out if it is a known user.
   * @param jsonAnswer - answer body which will be parsed as a Choice and then converted to a ChoiceDTO.
   * @return Response containing a QuestionValidationResponse object or containing a SegueErrorResponse .
   */
  @POST
  @Path("generateSpecification")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Turn an answer into a question specification.")
  public Response generateSpecification(@Context final HttpServletRequest request, final String jsonAnswer) {
    if (null == jsonAnswer || jsonAnswer.isEmpty()) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
    }

    try {
      if (!isUserStaff(userManager, request)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    }

    try {
      ChoiceDTO answerFromClientDTO = questionManager.convertJsonAnswerToChoice(jsonAnswer);

      return questionManager.generateSpecification(answerFromClientDTO);
    } catch (IllegalArgumentException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Bad request - " + e.getMessage(), e);
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    } catch (ErrorResponseWrapper responseWrapper) {
      return responseWrapper.toResponse();
    }
  }

  // Move these out of the facade
  private static List<String> splitCsvStringQueryParam(final String queryParamCsv) {
    if (null != queryParamCsv && !queryParamCsv.isEmpty()) {
      return Arrays.asList(queryParamCsv.split(","));
    } else {
      return null;
    }
  }
}
