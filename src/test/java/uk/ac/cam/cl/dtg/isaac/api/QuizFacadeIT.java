/*
 * Copyright 2022 Matthew Trew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.*;

public class QuizFacadeIT extends IsaacIntegrationTest {

    private QuizFacade quizFacade;

    @Before
    public void setUp() throws Exception {
        // get an instance of the facade to test
        this.quizFacade = new QuizFacade(properties, logManager, contentManager, quizManager, userAccountManager,
                userAssociationManager, groupManager, quizAssignmentManager, assignmentService, quizAttemptManager,
                quizQuestionManager);
    }

    @Test
    public void createQuizAssignmentEndpoint_assignQuizAsTeacher_succeeds() throws NoCredentialsAvailableException,
            NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
            IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
            NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
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
    public void createQuizAssignmentEndpoint_assignQuizAsTutor_fails() throws NoCredentialsAvailableException,
            NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
            IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
            NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
        // Arrange
        // log in as Tutor, create request
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL,
                TEST_TUTOR_PASSWORD);
        HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
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

