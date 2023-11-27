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
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_IT_TEST_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ALICE_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ALICE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ALICE_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_BOB_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_BOB_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_BOB_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_CHARLIE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHERS_AB_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_PASSWORD;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_DATE_FORMAT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_MILLISECONDS_IN_SECOND;
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
    this.quizFacade = new QuizFacade(properties, logManager, contentManager, quizManager, userAccountManager,
        userAssociationManager, groupManager, quizAssignmentManager, assignmentService, quizAttemptManager,
        quizQuestionManager);
  }

  Date someFutureDate =
      new Date(System.currentTimeMillis() + NUMBER_SECONDS_IN_ONE_DAY * NUMBER_MILLISECONDS_IN_SECOND);
  Date somePastDate =
      new Date(System.currentTimeMillis() - NUMBER_SECONDS_IN_ONE_DAY * NUMBER_MILLISECONDS_IN_SECOND);


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
    public void getQuizzesAsTeacher_returnsAll() throws NoCredentialsAvailableException,
        NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
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
        throws
        NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
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
    public void getQuizzesAsStudent_returnsNonInvisibleToStudentQuizzes() throws
        NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
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
  class PreviewQuizEndpoint {
    @Test
    public void previewInvisibleToStudentQuizAsTeacher_succeeds()
        throws NoCredentialsAvailableException,
        NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
      // Arrange
      // log in as Teacher, create request
      LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
      HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
      replay(previewQuizRequest);

      // Act
      // make request
      Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
          QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID);

      // Assert
      // check status code is OK
      assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

      // check the quiz is returned for preview
      IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
      assertEquals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID, responseBody.getId());
    }

    @Test
    public void previewHiddenFromRoleStudentQuizAsTutor_fails()
        throws NoCredentialsAvailableException,
        NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
      // Arrange
      // log in as Tutor, create request
      LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
      HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
      replay(previewQuizRequest);

      // Act
      // make request
      Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
          QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID);

      // Assert
      // check status code is FORBIDDEN
      assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

      // check an error message was returned
      SegueErrorResponse responseBody = (SegueErrorResponse) previewQuizResponse.getEntity();
      assertEquals("You do not have the permissions to complete this action", responseBody.getErrorMessage());
    }

    @Test
    public void previewHiddenFromRoleTutorQuizAsTutor_fails()
        throws NoCredentialsAvailableException,
        NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
      // Arrange
      // log in as Tutor, create request
      LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
      HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
      replay(previewQuizRequest);

      // Act
      // make request
      Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
          QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID);

      // Assert
      // check status code is FORBIDDEN
      assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

      // check an error message was returned
      SegueErrorResponse responseBody = (SegueErrorResponse) previewQuizResponse.getEntity();
      assertEquals("You do not have the permissions to complete this action", responseBody.getErrorMessage());
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
      assertTrue(responseBody.stream()
          .anyMatch(q -> q.getQuizId().equals(QUIZ_TEST_QUIZ_ID)));
    }
  }

  @Nested
  class GetQuizAssignment {
    @Test
    public void noQuizId_badRequest() {
      HttpServletRequest getQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
      replay(getQuizAssignmentRequest);

      Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, null);

      assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentResponse.getStatus());

      assertEquals("You must provide a valid test assignment id.",
          getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest getQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAssignmentRequest);

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, 1L);

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

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, 1L);

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

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, 1L);

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

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, 1L);

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

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, 1L);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentResponse.getStatus());

        QuizAssignmentDTO responseBody =
            (QuizAssignmentDTO) getQuizAssignmentResponse.getEntity();
        assertEquals(1L, responseBody.getId());
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

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, 1L);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentResponse.getStatus());

        QuizAssignmentDTO responseBody =
            (QuizAssignmentDTO) getQuizAssignmentResponse.getEntity();
        assertEquals(1L, responseBody.getId());
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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        assertEquals("You must provide a valid test assignment and user id id.",
            getQuizAssignmentAttemptResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest getQuizAssignmentAttemptRequest = createNiceMock(HttpServletRequest.class);
        replay(getQuizAssignmentAttemptRequest);

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_ALICE_ID);

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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_ALICE_ID);

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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_ALICE_ID);

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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_ALICE_ID);

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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_ID);

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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_CHARLIE_ID);

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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_BOB_ID);

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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        QuizAttemptFeedbackDTO responseBody =
            (QuizAttemptFeedbackDTO) getQuizAssignmentAttemptResponse.getEntity();
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
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, 1L, TEST_STUDENT_ALICE_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        QuizAttemptFeedbackDTO responseBody =
            (QuizAttemptFeedbackDTO) getQuizAssignmentAttemptResponse.getEntity();
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
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                null);

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
            new QuizAssignmentDTO(null, "not_a_quiz", null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
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
        Date somePastDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

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

          QuizAssignmentDTO responseBody =
              (QuizAssignmentDTO) createQuizAssignmentResponse.getEntity();
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

        try (Response createQuizAssignmentResponse =
            quizFacade.createQuizAssignment(createQuizAssignmentRequest, assignmentRequest)) {

          assertEquals(Response.Status.OK.getStatusCode(), createQuizAssignmentResponse.getStatus());

          QuizAssignmentDTO responseBody =
              (QuizAssignmentDTO) createQuizAssignmentResponse.getEntity();
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
    @Test
    public void missingQuizAssignmentId() {
      HttpServletRequest cancelQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
      replay(cancelQuizAssignmentRequest);

      try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest, null)) {

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

        assertEquals("You must provide a valid test assignment id.",
            cancelQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void anonymousUser() {
        HttpServletRequest cancelQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest, 1L)) {

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

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest, 1L)) {

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

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest, 1L)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              cancelQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void teacherNotGroupManager() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest cancelQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(cancelQuizAssignmentRequest);

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest, 1L)) {

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

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest, 2L)) {

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

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest, 3L)) {

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
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, null, quizAssignmentDto)) {

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
            new QuizAssignmentDTO(1L, null, null, null, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

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
            new QuizAssignmentDTO(null, "_quiz_test", null, null, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("Those fields are not editable.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void changingGroupId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, 1L, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("Those fields are not editable.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void changingOwnerId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, 1L, null, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("Those fields are not editable.",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void changingCreationDateId() {
        HttpServletRequest updateQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, new Date(), null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

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
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, somePastDate,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("You can only extend due dates into the future.",
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
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

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
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

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
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertEquals("You do not have the permissions to complete this action",
              updateQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
        }
      }

      @Test
      public void teacherNotGroupManager() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

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
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, someFutureDate,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void validFeedbackModeUpdate() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
          AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
          AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
          MFARequiredButNotConfiguredException {
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void validDueDateUpdateAsAdmin() throws JsonProcessingException {
        Cookie adminSessionCookie = createManualCookieForAdmin();
        HttpServletRequest updateQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
        replay(updateQuizAssignmentRequest);
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, someFutureDate,
                null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

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
            new QuizAssignmentDTO(null, null, null, null, null, null,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, 1L, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
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
    int sessionExpiryTimeInSeconds = 300;

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, sessionExpiryTimeInSeconds);
    String sessionExpiryDate = sessionDateFormat.format(calendar.getTime());

    Map<String, String> sessionInformation =
        userAuthenticationManager.prepareSessionInformation(userId, "0", sessionExpiryDate, hmacKey, null);
    return userAuthenticationManager.createAuthCookie(sessionInformation, sessionExpiryTimeInSeconds);
  }
}
