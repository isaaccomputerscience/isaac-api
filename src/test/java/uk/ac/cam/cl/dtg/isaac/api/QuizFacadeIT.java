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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHER_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ALICE_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ALICE_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_BOB_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_BOB_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHERS_AB_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTORS_AB_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_PASSWORD;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
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

  @Nested
  class CreateQuizAssignmentEndpoint {
    @Test
    public void assignQuizAsTeacher_succeeds() throws NoCredentialsAvailableException,
        NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
      // Arrange
      // log in as Teacher, create request
      LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
      HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
      replay(assignQuizRequest);

      QuizAssignmentDTO quizAssignmentDTO = new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID,
          TEST_TEACHER_ID, TEST_TEACHERS_AB_GROUP_ID, new Date(), DateUtils.addDays(new Date(), 5),
          QuizFeedbackMode.DETAILED_FEEDBACK);

      // Act
      // make request
      Response createQuizResponse = quizFacade.createQuizAssignment(assignQuizRequest, quizAssignmentDTO);

      // Assert
      // check status code is OK
      assertEquals(Response.Status.OK.getStatusCode(), createQuizResponse.getStatus());

      // check the quiz was assigned successfully
      QuizAssignmentDTO responseBody = (QuizAssignmentDTO) createQuizResponse.getEntity();
      assertEquals(TEST_TEACHERS_AB_GROUP_ID, (long) responseBody.getGroupId());
    }

    @Test
    public void assignQuizAsTutor_fails() throws NoCredentialsAvailableException,
        NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
      // Arrange
      // log in as Tutor, create request
      LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
      HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
      replay(assignQuizRequest);

      QuizAssignmentDTO quizAssignmentDTO = new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID,
          TEST_TUTOR_ID, TEST_TUTORS_AB_GROUP_ID, new Date(), DateUtils.addDays(new Date(), 5),
          QuizFeedbackMode.DETAILED_FEEDBACK);

      // Act
      // make request
      Response createQuizResponse = quizFacade.createQuizAssignment(assignQuizRequest, quizAssignmentDTO);

      // Assert
      // check status code is FORBIDDEN
      assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createQuizResponse.getStatus());

      // check an error message was returned
      SegueErrorResponse responseBody = (SegueErrorResponse) createQuizResponse.getEntity();
      assertEquals("You do not have the permissions to complete this action", responseBody.getErrorMessage());
    }
  }

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
    public void getAssignedQuizzesWithoutLogin_fails() {
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
    public void getFreeAttemptsWithoutLogin_fails() {
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

    @Test
    public void anonymousUser_unauthorized() {
      HttpServletRequest getQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
      replay(getQuizAssignmentRequest);

      Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, 1L);

      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAssignmentResponse.getStatus());

      assertEquals("You must be logged in to access this resource.",
          getQuizAssignmentResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void student_forbidden() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
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
    public void tutor_forbidden() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
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
    public void teacherNotGroupManager_forbidden()
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
          .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null));
    }

    @Disabled("Admin login requires MFA")
    @Test
    public void admin_feedbackOnlyForStudentsAllowingAccess()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult adminLogin = loginAs(httpSession, TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
      HttpServletRequest getQuizAssignmentRequest = createRequestWithCookies(new Cookie[] {adminLogin.cookie});
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
          .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null));
    }
  }
}
