/**
 * Copyright 2021 Raspberry Pi Foundation
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

package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

class QuizAttemptManagerTest extends AbstractManagerTest {
  private static final Long TEST_ID = 0xC0000000000L;
  private QuizAttemptManager quizAttemptManager;

  private IQuizAttemptPersistenceManager quizAttemptPersistenceManager;

  @BeforeEach
  public void setUp() {
    quizAttemptPersistenceManager = createMock(IQuizAttemptPersistenceManager.class);

    quizAttemptManager = new QuizAttemptManager(quizAttemptPersistenceManager);

    replay(quizAttemptPersistenceManager);
  }

  @Test
  void fetchOrCreateWithExistingAttempt() throws AttemptCompletedException, SegueDatabaseException {
    withMock(quizAttemptPersistenceManager, forStudentAssignmentReturn(studentAttempt));

    QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreate(studentAssignment, student);
    assertEquals(studentAttempt, attempt);
  }

  @Test
  void fetchOrCreateWithExistingCompletedAttemptFails() {
    withMock(quizAttemptPersistenceManager, forStudentAssignmentReturn(completedAttempt));

    assertThrows(AttemptCompletedException.class, () -> quizAttemptManager.fetchOrCreate(studentAssignment, student));
  }

  @Test
  void fetchOrCreateCreatesNewAttempt() throws AttemptCompletedException, SegueDatabaseException {
    withMock(quizAttemptPersistenceManager,
        forStudentAssignmentReturn(null),
        m -> expect(
            m.saveAttempt(attemptMatcher(student.getId(), studentAssignment.getId(), studentAssignment.getQuizId())))
            .andReturn(TEST_ID));

    QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreate(studentAssignment, student);
    assertEquals(TEST_ID, attempt.getId());
  }

  @Test
  void fetchOrCreateFreeQuizWithExistingAttempt() throws SegueDatabaseException {
    withMock(quizAttemptPersistenceManager, forStudentQuizReturn(Collections.singletonList(this.ownAttempt)));

    QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreateFreeQuiz(studentQuiz, student);
    assertEquals(ownAttempt, attempt);
  }

  @Test
  void fetchOrCreateFreeQuizWithExistingCompletedAttemptCreatesNewAttempt() throws SegueDatabaseException {
    withMock(quizAttemptPersistenceManager,
        forStudentQuizReturn(Collections.singletonList(completedAttempt)),
        returnTestIdForSaveAttempt());

    QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreateFreeQuiz(studentQuiz, student);
    assertEquals(TEST_ID, attempt.getId());
  }

  @Test
  void fetchOrCreateFreeQuizCreatesNewAttempt() throws SegueDatabaseException {
    withMock(quizAttemptPersistenceManager,
        forStudentQuizReturn(Collections.emptyList()),
        returnTestIdForSaveAttempt());

    QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreateFreeQuiz(studentQuiz, student);
    assertEquals(TEST_ID, attempt.getId());
  }

  @Test
  void augmentAssignmentsFor() throws SegueDatabaseException {
    withMock(quizAttemptPersistenceManager,
        m -> expect(
            m.getByQuizAssignmentIdsAndUserId(Collections.singletonList(studentAssignment.getId()), student.getId()))
            .andReturn(Collections.singletonMap(studentAssignment.getId(), studentAttempt)));
    quizAttemptManager.augmentAssignmentsFor(student, Collections.singletonList(studentAssignment));

    assertEquals(studentAttempt, studentAssignment.getAttempt());
  }

  private MockConfigurer<IQuizAttemptPersistenceManager> forStudentAssignmentReturn(QuizAttemptDTO attempt) {
    return m -> expect(m.getByQuizAssignmentIdAndUserId(studentAssignment.getId(), student.getId())).andReturn(attempt);
  }

  private MockConfigurer<IQuizAttemptPersistenceManager> forStudentQuizReturn(List<QuizAttemptDTO> attempts) {
    return m -> expect(m.getByQuizIdAndUserId(studentQuiz.getId(), student.getId())).andReturn(attempts);
  }

  private MockConfigurer<IQuizAttemptPersistenceManager> returnTestIdForSaveAttempt() {
    return m -> expect(m.saveAttempt(attemptMatcher(student.getId(), null, studentQuiz.getId())))
        .andReturn(TEST_ID);
  }

  private static QuizAttemptDTO attemptMatcher(final Long userId, final Long assignmentId, final String quizId) {
    EasyMock.reportMatcher(new IArgumentMatcher() {
      @Override
      public boolean matches(Object argument) {
        if (argument instanceof QuizAttemptDTO) {
          QuizAttemptDTO attempt = (QuizAttemptDTO) argument;
          return attempt.getUserId().equals(userId)
              && Objects.equals(attempt.getQuizAssignmentId(), assignmentId)
              && attempt.getQuizId().equals(quizId)
              && Duration.between(Instant.now(), attempt.getStartDate()).toMillis() < 1000;
        }
        return false;
      }

      @Override
      public void appendTo(StringBuffer buffer) {
        buffer.append("attempt(userId=" + userId + ", assignmentId=" + assignmentId + ", quizId=" + quizId + ")");
      }
    });
    return null;
  }

}
