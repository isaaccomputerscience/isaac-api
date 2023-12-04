/**
 * Copyright 2022 Matthew Trew
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

package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHERS_BC_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHER_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FEEDBACK_MODE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FREE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_BOB_COMPLETE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_BOB_FOR_SET_COMPLETE_TEST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_BOB_FREE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_CANCELLED_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_EXPIRED_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_FEEDBACK_MODE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_FIRST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_SECOND_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_NON_EXISTENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_SECOND_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_SET_INCOMPLETE_TEST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_IT_TEST_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_HIDDEN_FROM_TUTORS_QUESTION_FIRST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_QUESTION_FIRST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_UNKNOWN_QUESTION_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_NON_EXISTENT_USER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ALICE_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ALICE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ALICE_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_BOB_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_BOB_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_BOB_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_CHARLIE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ERIKA_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ERIKA_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ERIKA_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHERS_AB_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.UNKNOWN_QUIZ_ID;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_DATE_FORMAT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_MILLISECONDS_IN_SECOND;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_FIVE_MINUTES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_DAY;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizUserFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuizSummaryDTO;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public class QuizFacadeIT extends IsaacIntegrationTest {

  private QuizFacade quizFacade;

  @BeforeEach
  public void setUp() throws Exception {
    // get an instance of the facade to test
    this.quizFacade =
        new QuizFacade(properties, logManager, contentManager, quizManager, userAccountManager, userAssociationManager,
            groupManager, quizAssignmentManager, assignmentService, quizAttemptManager, quizQuestionManager);
  }

  Date someFutureDate =
      new Date(System.currentTimeMillis() + NUMBER_SECONDS_IN_ONE_DAY * NUMBER_MILLISECONDS_IN_SECOND);
  Date somePastDate = new Date(System.currentTimeMillis() - NUMBER_SECONDS_IN_ONE_DAY * NUMBER_MILLISECONDS_IN_SECOND);


  @Nested
  class GetAvailableQuizzesEndpoint {
    @Test
    public void getQuizzesWithoutLogin_fails() {
      HttpServletRequest getQuizzesRequest = createNiceMock(HttpServletRequest.class);
      replay(getQuizzesRequest);

      Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), getQuizzesRequest);

      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizzesResponse.getStatus());

      assertEquals("You must be logged in to access this resource.",
          getQuizzesResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void getQuizzesAsTeacher_returnsAll()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      // Arrange
      // log in as Teacher, create request
      LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
      HttpServletRequest getQuizzesRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
      replay(getQuizzesRequest);

      // Act
      // make request
      Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), getQuizzesRequest);

      // Assert
      // check status code is OK
      assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

      // check all quizzes are returned as available
      @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
          (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
      assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_TEST_QUIZ_ID)));
      assertTrue(
          responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)));
      assertTrue(
          responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)));
    }

    /**
     * Tests that quizzes with visibleToStudents=false and hiddenFromRoles=[TUTOR] are not considered available to a tutor.
     */
    @Test
    public void getQuizzesAsTutor_returnsNonInvisibleToStudentOrHiddenFromRoleQuizzes()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      // Arrange
      // log in as Tutor, create request
      LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
      HttpServletRequest getQuizzesRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
      replay(getQuizzesRequest);

      // Act
      // make request
      Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), getQuizzesRequest);

      // Assert
      // check status code is OK
      assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

      // check invisible-to-student and hidden-from-tutor-role quizzes are not returned as available
      @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
          (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
      assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_TEST_QUIZ_ID)));
      assertFalse(
          responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)));
      assertFalse(
          responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)));
    }

    @Test
    public void getQuizzesAsStudent_returnsNonInvisibleToStudentQuizzes()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      // Arrange
      // log in as Tutor, create request
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
      HttpServletRequest getQuizzesRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(getQuizzesRequest);

      // Act
      // make request
      Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), getQuizzesRequest);

      // Assert
      // check status code is OK
      assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

      // check invisible-to-student and hidden-from-tutor-role quizzes are not returned as available
      @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
          (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
      assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_TEST_QUIZ_ID)));
      assertFalse(
          responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)));
      assertFalse(
          responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)));
    }
  }

  @Nested
  class GetAssignedQuizzes {
    @Test
    public void anonymousUser_unauthorised() {
      HttpServletRequest assignedQuizRequest = createNiceMock(HttpServletRequest.class);
      replay(assignedQuizRequest);

      Response getAssignedQuizzesResponse = quizFacade.getAssignedQuizzes(assignedQuizRequest);

      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getAssignedQuizzesResponse.getStatus());

      assertEquals("You must be logged in to access this resource.",
          getAssignedQuizzesResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void studentAssignedQuizzes_noAssignments()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
      HttpServletRequest assignedQuizRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(assignedQuizRequest);

      Response getAssignedQuizzesResponse = quizFacade.getAssignedQuizzes(assignedQuizRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getAssignedQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
          (List<QuizAssignmentDTO>) getAssignedQuizzesResponse.getEntity();
      assertTrue(responseBody.isEmpty());
    }

    @Test
    public void studentAssignedQuizzes_oneAssignment()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
      HttpServletRequest assignedQuizRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(assignedQuizRequest);

      Response getAssignedQuizzesResponse = quizFacade.getAssignedQuizzes(assignedQuizRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getAssignedQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
          (List<QuizAssignmentDTO>) getAssignedQuizzesResponse.getEntity();
      assertTrue(responseBody.stream().anyMatch(q -> q.getQuizId().equals(QUIZ_TEST_QUIZ_ID)));
    }

    @Test
    public void teacherAssignedQuizzes_noAssignments()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
      HttpServletRequest assignedQuizRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
      replay(assignedQuizRequest);

      Response getAssignedQuizzesResponse = quizFacade.getAssignedQuizzes(assignedQuizRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getAssignedQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
          (List<QuizAssignmentDTO>) getAssignedQuizzesResponse.getEntity();
      assertTrue(responseBody.isEmpty());
    }
  }

  @Nested
  class GetFreeAttempts {
    @Test
    public void anonymousUser_unauthorised() {
      HttpServletRequest getFreeAttemptsRequest = createNiceMock(HttpServletRequest.class);
      replay(getFreeAttemptsRequest);

      Response getFreeAttemptsResponse = quizFacade.getFreeAttempts(getFreeAttemptsRequest);

      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getFreeAttemptsResponse.getStatus());

      assertEquals("You must be logged in to access this resource.",
          getFreeAttemptsResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void studentFreeAttempts_noAttempts()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
      HttpServletRequest getFreeAttemptsRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(getFreeAttemptsRequest);

      Response getFreeAttemptsResponse = quizFacade.getFreeAttempts(getFreeAttemptsRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getFreeAttemptsResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAttemptDTO> responseBody =
          (List<QuizAttemptDTO>) getFreeAttemptsResponse.getEntity();
      assertTrue(responseBody.isEmpty());
    }

    @Test
    public void studentFreeAttempts_oneAssignedAttempt()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
      HttpServletRequest getFreeAttemptsRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(getFreeAttemptsRequest);

      Response getFreeAttemptsResponse = quizFacade.getFreeAttempts(getFreeAttemptsRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getFreeAttemptsResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAttemptDTO> responseBody =
          (List<QuizAttemptDTO>) getFreeAttemptsResponse.getEntity();
      assertTrue(responseBody.isEmpty());
    }

    @Test
    public void studentFreeAttempts_oneFreeAttempt()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
      HttpServletRequest getFreeAttemptsRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(getFreeAttemptsRequest);

      Response getFreeAttemptsResponse = quizFacade.getFreeAttempts(getFreeAttemptsRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getFreeAttemptsResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAttemptDTO> responseBody =
          (List<QuizAttemptDTO>) getFreeAttemptsResponse.getEntity();
      assertTrue(responseBody.stream().anyMatch(q -> q.getQuizId().equals(QUIZ_TEST_QUIZ_ID)));
    }
  }

  @Nested
  class GetQuizAssignment {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void noQuizId() {
        HttpServletRequest getQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentResponse.getStatus());

        assertEquals("You must provide a valid test assignment id.",
            getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void cancelledAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse =
            quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_CANCELLED_ID);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentResponse.getStatus());

        assertEquals("This assignment has been cancelled.",
            getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest getQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAssignmentResponse.getStatus());

        assertEquals("You must be logged in to access this resource.",
            getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void student() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest getQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentResponse.getStatus());

        assertEquals("You do not have the permissions to complete this action",
            getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void tutor() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest getQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentResponse.getStatus());

        assertEquals("You do not have the permissions to complete this action",
            getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void teacherNotGroupManager()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentResponse.getStatus());

        assertEquals("You can only view assignments to groups you own or manage.",
            getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Nested
    class ValidUser {
      @Test
      public void teacherIsGroupManager_feedbackOnlyForStudentsAllowingAccess()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentResponse.getStatus());

        QuizAssignmentDTO responseBody = (QuizAssignmentDTO) getQuizAssignmentResponse.getEntity();
        assertEquals(QUIZ_ASSIGNMENT_ID, responseBody.getId());
        assertTrue(responseBody.getUserFeedback().stream()
            .anyMatch(f -> f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null));
        assertTrue(responseBody.getUserFeedback().stream()
            .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() == null));
        assertFalse(responseBody.getUserFeedback().stream()
            .anyMatch(f -> f.getUser().isAuthorisedFullAccess() && f.getFeedback() == null));
        assertFalse(responseBody.getUserFeedback().stream()
            .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null));
      }

      @Test
      public void admin_feedbackAccessAllForStudents() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest getQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentResponse.getStatus());

        QuizAssignmentDTO responseBody = (QuizAssignmentDTO) getQuizAssignmentResponse.getEntity();
        assertEquals(QUIZ_ASSIGNMENT_ID, responseBody.getId());
        assertTrue(responseBody.getUserFeedback().stream()
            .anyMatch(f -> f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null));
        assertFalse(responseBody.getUserFeedback().stream()
            .anyMatch(f -> f.getUser().isAuthorisedFullAccess() && f.getFeedback() == null));
        assertFalse(responseBody.getUserFeedback().stream()
            .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() == null));
        assertFalse(responseBody.getUserFeedback().stream()
            .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null));
      }
    }
  }

  @Nested
  class GetQuizAssignmentAttempt {
    @Nested
    class BadRequestMissingData {
      @Test
      public void noQuizId_badRequest() {
        HttpServletRequest getQuizAssignmentAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, null, TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("You must provide a valid test assignment and user id id.",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void noUserId_badRequest() {
        HttpServletRequest getQuizAssignmentAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("You must provide a valid test assignment and user id id.",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void cancelledAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_CANCELLED_ID,
                TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentResponse.getStatus());

        assertEquals("This assignment has been cancelled.",
            getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest getQuizAssignmentAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("You must be logged in to access this resource.",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void student() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest getQuizAssignmentAttemptRequest =
            createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("You do not have the permissions to complete this action",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void tutor() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest getQuizAssignmentAttemptRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("You do not have the permissions to complete this action",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void teacherNotGroupManager()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("You can only view assignments to groups you own or manage.",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Nested
    class CorrectGroupOwnerButInvalidTarget {
      @Test
      public void targetStudentNotInGroup()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID, TEST_STUDENT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("That student is not in the group that was assigned this test.",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void targetStudentHasPrivacyRestrictions()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                TEST_STUDENT_CHARLIE_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("You do not have access to that student's data.",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }

      @Test
      public void targetStudentHasNotCompletedAssignment()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                TEST_STUDENT_BOB_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("That student has not completed this test assignment.",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Nested
    class ValidRequest {
      @Test
      public void targetStudentHasCompletedAssignment()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest getQuizAssignmentAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        QuizAttemptFeedbackDTO responseBody = (QuizAttemptFeedbackDTO) getQuizAssignmentAttemptResponse.getEntity();
        assertEquals(TEST_STUDENT_ALICE_ID, responseBody.getUser().getId());
        assertEquals(TEST_STUDENT_ALICE_ID, responseBody.getAttempt().getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getAttempt().getQuizId());
      }


      @Test
      public void targetStudentHasNotCompletedAssignmentAsAdmin() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest getQuizAssignmentAttemptRequest =
            createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        QuizAttemptFeedbackDTO responseBody = (QuizAttemptFeedbackDTO) getQuizAssignmentAttemptResponse.getEntity();
        assertEquals(TEST_STUDENT_ALICE_ID, responseBody.getUser().getId());
        assertEquals(TEST_STUDENT_ALICE_ID, responseBody.getAttempt().getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getAttempt().getQuizId());
      }
    }
  }

  @Nested
  class CreateQuizAssignment {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void assignmentMissingQuizId() {
        HttpServletRequest createQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, null, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("A required field was missing. Must provide group and test ids and a test feedback mode.",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void assignmentMissingGroupId() {
        HttpServletRequest createQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, null, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("A required field was missing. Must provide group and test ids and a test feedback mode.",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void assignmentMissingFeedbackMode() {
        HttpServletRequest createQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate, null);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("A required field was missing. Must provide group and test ids and a test feedback mode.",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void unknownQuizId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest createQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, UNKNOWN_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.NOT_FOUND.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("This test has become unavailable.",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void dueDateInPast() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest createQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, somePastDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("You cannot set a quiz with a due date in the past.",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void duplicateAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest createQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, QUIZ_FACADE_IT_TEST_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("You cannot reassign a test until the due date has passed.",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest createQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void student() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest createQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void tutor() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest createQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void teacherNotGroupManager()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest createQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createQuizAssignmentResponse.getStatus());

          assertEquals("You can only set assignments to groups you own or manage.",
              createQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class ValidUser {
      @Test
      public void validAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest createQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.OK.getStatusCode(), createQuizAssignmentResponse.getStatus());

          QuizAssignmentDTO responseBody = (QuizAssignmentDTO) createQuizAssignmentResponse.getEntity();
          assertNotNull(responseBody.getId());
          assertEquals(TEST_TEACHER_ID, responseBody.getOwnerUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(TEST_TEACHERS_AB_GROUP_ID, responseBody.getGroupId());
          assertNotNull(responseBody.getCreationDate());
          assertEquals(someFutureDate, responseBody.getDueDate());
        }
      }

      @Test
      public void validAssignmentAsAdmin() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest createQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(createQuizAssignmentRequest);
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, DAVE_TEACHERS_BC_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.OK.getStatusCode(), createQuizAssignmentResponse.getStatus());

          QuizAssignmentDTO responseBody = (QuizAssignmentDTO) createQuizAssignmentResponse.getEntity();
          assertNotNull(responseBody.getId());
          assertEquals(TEST_ADMIN_ID, responseBody.getOwnerUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(DAVE_TEACHERS_BC_GROUP_ID, responseBody.getGroupId());
          assertNotNull(responseBody.getCreationDate());
          assertEquals(someFutureDate, responseBody.getDueDate());
        }
      }
    }
  }

  @Nested
  class CancelQuizAssignment {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void missingQuizAssignmentId() {
        HttpServletRequest cancelQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertEquals("You must provide a valid test assignment id.",
              cancelQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void cancelledAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest cancelQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_CANCELLED_ID)) {


          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertEquals("This assignment is already cancelled.",
              cancelQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest cancelQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              cancelQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void student() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest cancelQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              cancelQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void tutor() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest cancelQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              cancelQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void teacherNotGroupManager()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest cancelQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertEquals("You can only cancel assignments to groups you own or manage.",
              cancelQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class ValidUser {
      @Test
      public void validRequest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest cancelQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_FIRST_ID)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertNull(cancelQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void validRequestAsAdmin() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest cancelQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_SECOND_ID)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertNull(cancelQuizAssignmentResponse.getEntity());
        }
      }
    }
  }

  @Nested
  class UpdateQuizAssignment {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void missingQuizAssignmentId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, null,
            quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("You must provide a valid test assignment id.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void changingAssignmentId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(QUIZ_ASSIGNMENT_ID, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("Those fields are not editable.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void changingQuizId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("Those fields are not editable.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void changingGroupId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, 1L, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("Those fields are not editable.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void changingOwnerId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, 1L, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("Those fields are not editable.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void changingCreationDateId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, new Date(), null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("Those fields are not editable.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void pastDueDate() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, somePastDate, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("You can only extend due dates into the future.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void cancelledAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, someFutureDate, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_CANCELLED_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("This assignment is already cancelled.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void student() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void tutor() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void teacherNotGroupManager()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("You can only updates assignments to groups you own or manage.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class ValidUser {
      @Test
      public void validDueDateUpdate() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, someFutureDate, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void validFeedbackModeUpdate()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null, QuizFeedbackMode.OVERALL_MARK);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void validDueDateUpdateAsAdmin() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, someFutureDate, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void validFeedbackModeUpdateAsAdmin() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null, QuizFeedbackMode.OVERALL_MARK);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }
    }
  }

  @Nested
  class PreviewQuiz {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void nullId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), previewQuizResponse.getStatus());

          assertEquals("You must provide a valid test id.",
              previewQuizResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void emptyId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            "")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), previewQuizResponse.getStatus());

          assertEquals("You must provide a valid test id.",
              previewQuizResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void unknownId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            UNKNOWN_QUIZ_ID)) {

          assertEquals(Response.Status.NOT_FOUND.getStatusCode(), previewQuizResponse.getStatus());

          assertEquals("This test has become unavailable.",
              previewQuizResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class StandardQuizPermissions {
      @Test
      public void anonymousUser() {
        HttpServletRequest previewQuizRequest = createNiceMock(HttpServletRequest.class);
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), previewQuizResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              previewQuizResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void student() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              previewQuizResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void tutor() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

          IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getId());
        }
      }

      @Test
      public void teacher() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

          IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getId());
        }
      }
    }

    @Nested
    class RestrictedQuizPermissions {
      @Test
      public void hiddenFromStudentsAsTutor()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              previewQuizResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void hiddenFromTutorsAsTutor()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              previewQuizResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void hiddenFromStudentsAsTeacher()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

          IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
          assertEquals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID, responseBody.getId());
        }
      }

      @Test
      public void hiddenFromTutorsAsTeacher()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(previewQuizRequest);

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

          IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
          assertEquals(QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID, responseBody.getId());
        }
      }
    }
  }

  @Nested
  class StartQuizAttempt {
    @Nested
    class InvalidUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest startQuizAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(startQuizAttemptRequest);

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), startQuizAttemptResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              startQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void studentNotInGroup() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest startQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startQuizAttemptRequest);

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startQuizAttemptResponse.getStatus());

          assertEquals("You are not a member of a group to which this test is assigned.",
              startQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void missingId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest startQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startQuizAttemptRequest);

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), startQuizAttemptResponse.getStatus());

          assertEquals("You must provide a valid test assignment id.",
              startQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void cancelledAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest startQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startQuizAttemptRequest);

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_CANCELLED_ID)) {

          assertEquals(Response.Status.GONE.getStatusCode(), startQuizAttemptResponse.getStatus());

          assertEquals("This test assignment has been cancelled.",
              startQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void expiredAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest startQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startQuizAttemptRequest);

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_EXPIRED_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startQuizAttemptResponse.getStatus());

          assertEquals("The due date for this test has passed.",
              startQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void alreadyAttempted() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest startQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startQuizAttemptRequest);

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startQuizAttemptResponse.getStatus());

          assertEquals("You have already completed your attempt for this test.",
              startQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Test
    public void validRequest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
      HttpServletRequest startQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(startQuizAttemptRequest);

      try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
          startQuizAttemptRequest, QUIZ_ASSIGNMENT_ID)) {

        assertEquals(Response.Status.OK.getStatusCode(), startQuizAttemptResponse.getStatus());

        QuizAttemptDTO responseBody = (QuizAttemptDTO) startQuizAttemptResponse.getEntity();
        assertEquals(TEST_STUDENT_BOB_ID, responseBody.getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
        assertEquals(QUIZ_ASSIGNMENT_ID, responseBody.getQuizAssignmentId());
        assertNotNull(responseBody.getQuiz());
        assertNotNull(responseBody.getQuizAssignment());
      }
    }
  }

  @Nested
  class StartFreeQuizAttempt {
    @Nested
    class InvalidUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest startFreeQuizAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(startFreeQuizAttemptRequest);

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              startFreeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void hiddenFromStudents() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ERIKA_EMAIL, TEST_STUDENT_ERIKA_PASSWORD);
        HttpServletRequest startFreeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startFreeQuizAttemptRequest);

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          assertEquals("Free attempts are not available for test quiz.",
              startFreeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void assignedQuiz() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest startFreeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startFreeQuizAttemptRequest);

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          assertEquals("This test has been assigned to you by a teacher. "
                  + "You can not attempt this test freely. If you have already done the test for your teacher, "
                  + "and want to do it again, ask your teacher to allow you another attempt.",
              startFreeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void missingId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ERIKA_EMAIL, TEST_STUDENT_ERIKA_PASSWORD);
        HttpServletRequest startFreeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startFreeQuizAttemptRequest);

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          assertEquals("You must provide a valid test id.",
              startFreeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void emptyId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ERIKA_EMAIL, TEST_STUDENT_ERIKA_PASSWORD);
        HttpServletRequest startFreeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startFreeQuizAttemptRequest);

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, "")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          assertEquals("You must provide a valid test id.",
              startFreeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void unknownId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ERIKA_EMAIL, TEST_STUDENT_ERIKA_PASSWORD);
        HttpServletRequest startFreeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(startFreeQuizAttemptRequest);

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, UNKNOWN_QUIZ_ID)) {

          assertEquals(Response.Status.NOT_FOUND.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          assertEquals("This test has become unavailable.",
              startFreeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Test
    public void validRequest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ERIKA_EMAIL, TEST_STUDENT_ERIKA_PASSWORD);
      HttpServletRequest startFreeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(startFreeQuizAttemptRequest);

      try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
          startFreeQuizAttemptRequest, QUIZ_TEST_QUIZ_ID)) {

        assertEquals(Response.Status.OK.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

        QuizAttemptDTO responseBody = (QuizAttemptDTO) startFreeQuizAttemptResponse.getEntity();
        assertEquals(TEST_STUDENT_ERIKA_ID, responseBody.getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
        assertNull(responseBody.getQuizAssignmentId());
        assertNotNull(responseBody.getQuiz());
        assertNull(responseBody.getQuizAssignment());
      }
    }
  }

  @Nested
  class GetQuizAttempt {
    @Nested
    class InvalidUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest getQuizAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAttemptRequest);

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAttemptResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              getQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void studentNotAttemptCreator()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest getQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptRequest);

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptResponse.getStatus());

          assertEquals("This is not your test attempt.",
              getQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class InvalidRequest {
      @Test
      public void cancelledAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest getQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptRequest);

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptResponse.getStatus());

          assertEquals("This test assignment has been cancelled.",
              getQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void expiredAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest getQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptRequest);

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptResponse.getStatus());

          assertEquals("The due date for this test has passed.",
              getQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void alreadyCompleted() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest getQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptRequest);

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptResponse.getStatus());

          assertEquals("You have completed this test.",
              getQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Test
    public void validRequest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
      HttpServletRequest getQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(getQuizAttemptRequest);

      try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptResponse.getStatus());

        QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptResponse.getEntity();
        assertEquals(TEST_STUDENT_ALICE_ID, responseBody.getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
        assertEquals(QUIZ_ASSIGNMENT_SECOND_ID, responseBody.getQuizAssignmentId());
        assertNotNull(responseBody.getQuiz());
        assertNotNull(responseBody.getQuizAssignment());
      }
    }
  }

  @Nested
  class GetQuizAttemptFeedback {
    @Nested
    class InvalidUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest getQuizAttemptFeedbackRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              getQuizAttemptFeedbackResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void studentNotAttemptCreator()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          assertEquals("This is not your test attempt.",
              getQuizAttemptFeedbackResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void teacher() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          assertEquals("This is not your test attempt.",
              getQuizAttemptFeedbackResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void admin() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          assertEquals("This is not your test attempt.",
              getQuizAttemptFeedbackResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class IncompleteAttempt {
      @Test
      public void expiredAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          assertEquals("You have not completed this test.",
              getQuizAttemptFeedbackResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void incompleteAttempt() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          assertEquals("You have not completed this test.",
              getQuizAttemptFeedbackResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class CompletedAttempt {
      @Test
      public void completedAttemptNoDueDate()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptFeedbackResponse.getEntity();
          assertEquals(TEST_STUDENT_ALICE_ID, responseBody.getUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(QUIZ_ASSIGNMENT_ID, responseBody.getQuizAssignmentId());
          assertNull(responseBody.getQuiz());
          assertNotNull(responseBody.getQuizAssignment());
          assertEquals(QuizFeedbackMode.NONE, responseBody.getFeedbackMode());
        }
      }

      @Test
      public void completedAttemptWithDueDate()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_BOB_COMPLETE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptFeedbackResponse.getEntity();
          assertEquals(TEST_STUDENT_BOB_ID, responseBody.getUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(QUIZ_ASSIGNMENT_EXPIRED_ID, responseBody.getQuizAssignmentId());
          assertNull(responseBody.getQuiz());
          assertNotNull(responseBody.getQuizAssignment());
          assertEquals(QuizFeedbackMode.NONE, responseBody.getFeedbackMode());
        }
      }

      @Test
      public void completedAttemptFeedbackMode()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FEEDBACK_MODE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptFeedbackResponse.getEntity();
          assertEquals(TEST_STUDENT_ALICE_ID, responseBody.getUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(QUIZ_ASSIGNMENT_FEEDBACK_MODE_ID, responseBody.getQuizAssignmentId());
          assertNotNull(responseBody.getQuiz());
          assertNotNull(responseBody.getQuizAssignment());
          assertEquals(QuizFeedbackMode.DETAILED_FEEDBACK, responseBody.getFeedbackMode());
        }
      }

      @Test
      public void freeAttempt() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest getQuizAttemptFeedbackRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(getQuizAttemptFeedbackRequest);

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_BOB_FREE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptFeedbackResponse.getEntity();
          assertEquals(TEST_STUDENT_BOB_ID, responseBody.getUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertNull(responseBody.getQuizAssignmentId());
          assertNotNull(responseBody.getQuiz());
          assertNull(responseBody.getQuizAssignment());
          assertEquals(QuizFeedbackMode.DETAILED_FEEDBACK, responseBody.getFeedbackMode());
        }
      }
    }
  }

  @Nested
  class CompleteQuizAttempt {
    @Nested
    class InvalidUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest completeQuizAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(completeQuizAttemptRequest);

        try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), completeQuizAttemptResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              completeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void studentNotAttemptCreator()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest completeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(completeQuizAttemptRequest);

        try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), completeQuizAttemptResponse.getStatus());

          assertEquals("You cannot complete someone else's test.",
              completeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void teacher() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest completeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(completeQuizAttemptRequest);

        try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), completeQuizAttemptResponse.getStatus());

          assertEquals("You cannot complete someone else's test.",
              completeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void admin() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest completeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(completeQuizAttemptRequest);

        try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), completeQuizAttemptResponse.getStatus());

          assertEquals("You cannot complete someone else's test.",
              completeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Test
    public void alreadyCompleted()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
      HttpServletRequest completeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(completeQuizAttemptRequest);

      try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), completeQuizAttemptResponse.getStatus());

        assertEquals("That test is already complete.",
            completeQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Test
    public void validRequest()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
      HttpServletRequest completeQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(completeQuizAttemptRequest);

      try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_BOB_FOR_SET_COMPLETE_TEST_ID)) {

        assertEquals(Response.Status.OK.getStatusCode(), completeQuizAttemptResponse.getStatus());

        QuizAttemptDTO responseBody = (QuizAttemptDTO) completeQuizAttemptResponse.getEntity();
        assertEquals(TEST_STUDENT_BOB_ID, responseBody.getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
        assertEquals(QUIZ_ASSIGNMENT_FEEDBACK_MODE_ID, responseBody.getQuizAssignmentId());
        assertNull(responseBody.getQuiz());
        assertNull(responseBody.getQuizAssignment());
      }
    }
  }

  @Nested
  class MarkIncompleteQuizAttempt {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void missingAssignmentId() {
        HttpServletRequest markIncompleteQuizAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            null, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("Missing quizAssignmentId.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void missingUserId() {
        HttpServletRequest markIncompleteQuizAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ID, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("Missing userId.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void unknownUserId()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ID, TEST_NON_EXISTENT_USER_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("No such user.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void cancelledAssignment()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_CANCELLED_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("This test assignment has been cancelled.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void expiredAssignment()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_EXPIRED_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("You cannot mark a test attempt as incomplete after the due date.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void userNotInGroup()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ID, TEST_STUDENT_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("That user is not in this group.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void assignmentAlreadyIncomplete()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_SECOND_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("That test is already incomplete.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void unknownAssignmentId()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_NON_EXISTENT_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("SegueDatabaseException whilst marking test attempt incomplete",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest markIncompleteQuizAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void student() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("You can only mark assignments incomplete for groups you own or manage.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void tutor() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("You can only mark assignments incomplete for groups you own or manage.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void teacherNotGroupManager()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          assertEquals("You can only mark assignments incomplete for groups you own or manage.",
              markIncompleteQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class ValidRequest {
      @Test
      public void validRequest()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_SET_INCOMPLETE_TEST_ID, TEST_STUDENT_ALICE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          QuizUserFeedbackDTO responseBody = (QuizUserFeedbackDTO) markIncompleteQuizAttemptResponse.getEntity();
          assertEquals(TEST_STUDENT_ALICE_ID, responseBody.getUser().getId());
          assertNotNull(responseBody.getFeedback());
        }
      }

      @Test
      public void validRequestPrivacyRestrictions()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_SET_INCOMPLETE_TEST_ID, TEST_STUDENT_CHARLIE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          QuizUserFeedbackDTO responseBody = (QuizUserFeedbackDTO) markIncompleteQuizAttemptResponse.getEntity();
          assertEquals(TEST_STUDENT_CHARLIE_ID, responseBody.getUser().getId());
          assertNull(responseBody.getFeedback());
        }
      }

      @Test
      public void validRequestAsAdmin() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest markIncompleteQuizAttemptRequest =
            createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(markIncompleteQuizAttemptRequest);

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ID, TEST_STUDENT_CHARLIE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          QuizUserFeedbackDTO responseBody = (QuizUserFeedbackDTO) markIncompleteQuizAttemptResponse.getEntity();
          assertEquals(TEST_STUDENT_CHARLIE_ID, responseBody.getUser().getId());
          assertNotNull(responseBody.getFeedback());
        }
      }
    }
  }

  @Nested
  class AnswerQuestion {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void nullAnswer() {
        HttpServletRequest answerQuestionRequest = createNiceMock(HttpServletRequest.class);
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "questionId", null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("No answer received.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void emptyAnswer() {
        HttpServletRequest answerQuestionRequest = createNiceMock(HttpServletRequest.class);
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "questionId", "")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("No answer received.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void nullQuestionId() {
        HttpServletRequest answerQuestionRequest = createNiceMock(HttpServletRequest.class);
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, null, "answer")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("Missing questionId.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void emptyQuestionId() {
        HttpServletRequest answerQuestionRequest = createNiceMock(HttpServletRequest.class);
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "", "answer")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("Missing questionId.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void unknownQuestionId() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest answerQuestionRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, QUIZ_TEST_UNKNOWN_QUESTION_ID, "answer")) {

          assertEquals(Response.Status.NOT_FOUND.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("No question object found for given id: " + QUIZ_TEST_UNKNOWN_QUESTION_ID,
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void questionNotInQuiz() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest answerQuestionRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, QUIZ_TEST_HIDDEN_FROM_TUTORS_QUESTION_FIRST_ID, "answer")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("This question is part of another test.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void completedAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest answerQuestionRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID, "questionId", "answer")) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("You have completed this test.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void cancelledAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest answerQuestionRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID, "questionId", "answer")) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("This test assignment has been cancelled.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void expiredAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest answerQuestionRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID, "questionId", "answer")) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("The due date for this test has passed.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void unknownAssignment() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest answerQuestionRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_NON_EXISTENT_ID, QUIZ_TEST_QUESTION_FIRST_ID, "answer")) {

          assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("SegueDatabaseException whilst submitting test answer",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest answerQuestionRequest = createNiceMock(HttpServletRequest.class);
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "questionId", "answer")) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void studentNotAttemptCreator()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest answerQuestionRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(answerQuestionRequest);

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "questionId", "answer")) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), answerQuestionResponse.getStatus());

          assertEquals("This is not your test attempt.",
              answerQuestionResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }
  }

  @Nested
  class AbandonQuizAttempt {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void missingAttemptId()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest cancelQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(cancelQuizAttemptRequest);

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          assertEquals("You must provide a valid test attempt id.",
              cancelQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void canOnlyCancelFreeAttempts()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest cancelQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(cancelQuizAttemptRequest);

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          assertEquals("You can only cancel attempts on tests you chose to take.",
              cancelQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void completedAttempt()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest cancelQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(cancelQuizAttemptRequest);

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_BOB_FREE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          assertEquals("You cannot cancel completed test attempts.",
              cancelQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest cancelQuizAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(cancelQuizAttemptRequest);

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              cancelQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void studentNotAttemptCreator()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest cancelQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(cancelQuizAttemptRequest);

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          assertEquals("You cannot cancel a test attempt for someone else.",
              cancelQuizAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Test
    public void validRequest()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
      HttpServletRequest cancelQuizAttemptRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(cancelQuizAttemptRequest);

      try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FREE_ID)) {

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelQuizAttemptResponse.getStatus());

        assertNull(cancelQuizAttemptResponse.getEntity());
      }
    }
  }

  @Nested
  class LogQuizSectionView {
    @Nested
    class BadRequestMissingOrInvalidData {
      @Test
      public void missingSectionNumber() {
        HttpServletRequest logQuizSectionViewRequest = createNiceMock(HttpServletRequest.class);
        replay(logQuizSectionViewRequest);

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), logQuizSectionViewResponse.getStatus());

          assertEquals("Missing sectionNumber.",
              logQuizSectionViewResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void cancelledAssignment()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest logQuizSectionViewRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(logQuizSectionViewRequest);

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID, 1)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), logQuizSectionViewResponse.getStatus());

          assertEquals("This test assignment has been cancelled.",
              logQuizSectionViewResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void expiredAssignment()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest logQuizSectionViewRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(logQuizSectionViewRequest);

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID, 1)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), logQuizSectionViewResponse.getStatus());

          assertEquals("The due date for this test has passed.",
              logQuizSectionViewResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void alreadyCompletedAssignment()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
        HttpServletRequest logQuizSectionViewRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(logQuizSectionViewRequest);

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID, 1)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), logQuizSectionViewResponse.getStatus());

          assertEquals("You have completed this test.",
              logQuizSectionViewResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest logQuizSectionViewRequest = createNiceMock(HttpServletRequest.class);
        replay(logQuizSectionViewRequest);

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, 1)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), logQuizSectionViewResponse.getStatus());

          assertEquals("You must be logged in to access this resource.",
              logQuizSectionViewResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void studentNotAttemptCreator()
          throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_BOB_EMAIL, TEST_STUDENT_BOB_PASSWORD);
        HttpServletRequest logQuizSectionViewRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
        replay(logQuizSectionViewRequest);

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, 1)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), logQuizSectionViewResponse.getStatus());

          assertEquals("This is not your test attempt.",
              logQuizSectionViewResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }
    }

    @Test
    public void validRequest()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_ALICE_EMAIL, TEST_STUDENT_ALICE_PASSWORD);
      HttpServletRequest logQuizSectionViewRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(logQuizSectionViewRequest);

      try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, 1)) {

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), logQuizSectionViewResponse.getStatus());

        assertNull(logQuizSectionViewResponse.getEntity());
      }
    }
  }

  /**
   * As the integration tests do not currently support MFA login, we cannot use the normal login process and have to
   * create cookies manually when testing admin accounts.
   *
   * @return a Cookie loaded with session information for the test admin user.
   * @throws JsonProcessingException if the cookie serialisation fails
   */
  public Cookie createManualCookieForAdmin() throws JsonProcessingException {
    SimpleDateFormat sessionDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
    String userId = String.valueOf(TEST_ADMIN_ID);
    String hmacKey = properties.getProperty(HMAC_SALT);
    int sessionExpiryTimeInSeconds = NUMBER_SECONDS_IN_FIVE_MINUTES;

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, sessionExpiryTimeInSeconds);
    String sessionExpiryDate = sessionDateFormat.format(calendar.getTime());

    Map<String, String> sessionInformation =
        userAuthenticationManager.prepareSessionInformation(userId, "0", sessionExpiryDate, hmacKey, null);
    return userAuthenticationManager.createAuthCookie(sessionInformation, sessionExpiryTimeInSeconds);
  }
}
