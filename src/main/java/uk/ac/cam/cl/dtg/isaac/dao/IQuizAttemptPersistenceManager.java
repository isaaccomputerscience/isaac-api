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

package uk.ac.cam.cl.dtg.isaac.dao;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public interface IQuizAttemptPersistenceManager {
  /**
   * Get the quiz attempt for a particular user in a particular assignment.
   *
   * @param quizAssignmentId The id of the quiz assignment.
   * @param userId           The id of the user.
   * @return The attempt, or null if no attempt exists.
   */
  QuizAttemptDTO getByQuizAssignmentIdAndUserId(Long quizAssignmentId, Long userId) throws SegueDatabaseException;

  /**
   * Save an Quiz attempt.
   *
   * @param attempt - assignment to save
   * @return internal database id for the saved attempt.
   * @throws SegueDatabaseException - if there is a problem saving the attempt in the database.
   */
  Long saveAttempt(QuizAttemptDTO attempt) throws SegueDatabaseException;

  /**
   * Get all quiz attempts for a particular user at a particular quiz.
   *
   * @param quizId The id of the quiz.
   * @param userId The id of the user.
   * @return The attempts.
   */
  List<QuizAttemptDTO> getByQuizIdAndUserId(String quizId, Long userId) throws SegueDatabaseException;

  /**
   * Get a quiz attempt by id.
   *
   * @param quizAttemptId The id of the attempt.
   * @return The attempt or an error if it doesn't exist.
   */
  QuizAttemptDTO getById(Long quizAttemptId) throws SegueDatabaseException;

  /**
   * Delete a quiz attempt from the database.
   *
   * @param quizAttemptId The id of the attempt to delete.
   */
  void deleteAttempt(Long quizAttemptId) throws SegueDatabaseException;

  /**
   * Mark a quiz complete (or incomplete) in the database.
   *
   * @param quizAttemptId       The id of the attempt to update.
   * @param newCompletionStatus If true sets the completed_date on the quiz attempt to the current date, if false sets
   *                                it to null
   * @return The new value for completedDate.
   */
  @Nullable
  Instant updateAttemptCompletionStatus(Long quizAttemptId, boolean newCompletionStatus) throws SegueDatabaseException;

  /**
   * Get the set of user IDs that have attempted and completed a given assignment.
   *
   * @param assignmentId The assignment.
   * @return A set of user IDs with completed attempts.
   */
  Set<Long> getCompletedUserIds(Long assignmentId) throws SegueDatabaseException;

  /**
   * Get the attempts for a list of assignments for a particular user.
   *
   * @param quizAssignmentIds The IDs of the assignments.
   * @param userId            The ID of the user.
   * @return A map of assignment ID to quiz attempts.
   */
  Map<Long, QuizAttemptDTO> getByQuizAssignmentIdsAndUserId(List<Long> quizAssignmentIds, Long userId)
      throws SegueDatabaseException;

  /**
   * Get the attempts for a user that are not associated with an assignment.
   *
   * @param userId The ID of the user.
   * @return A list of attempts with a null quizAssignmentId.
   */
  List<QuizAttemptDTO> getFreeAttemptsByUserId(Long userId) throws SegueDatabaseException;
}
