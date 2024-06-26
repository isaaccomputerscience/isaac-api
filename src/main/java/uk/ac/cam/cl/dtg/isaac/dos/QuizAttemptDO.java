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

package uk.ac.cam.cl.dtg.isaac.dos;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.DO_HASHCODE_PRIME;

import jakarta.annotation.Nullable;
import java.time.Instant;

/**
 * This class is the Domain Object used to store Quiz attempts in the isaac CMS.
 */
public class QuizAttemptDO {
  private Long id;
  private Long userId;
  private String quizId;
  @Nullable
  private Long quizAssignmentId;
  private Instant startDate;
  @Nullable
  private Instant completedDate;

  /**
   * Complete QuizAttemptDO constructor with all dependencies.
   *
   * @param id               - unique id for the quiz attempt
   * @param userId           - The user making this attempt
   * @param quizId           - The quiz being attempted.
   * @param quizAssignmentId - The quiz assignment, or null if this is a self-selected quiz.
   * @param startDate        - When this attempt began.
   * @param completedDate    - When this attempt was marked complete, or null if not yet completed.
   */
  public QuizAttemptDO(final Long id, final Long userId, final String quizId, final Long quizAssignmentId,
                       final Instant startDate, @Nullable final Instant completedDate) {
    this.id = id;
    this.userId = userId;
    this.quizId = quizId;
    this.quizAssignmentId = quizAssignmentId;
    this.startDate = startDate;
    this.completedDate = completedDate;
  }

  /**
   * Default constructor required for AutoMapping.
   */
  public QuizAttemptDO() {

  }

  @Override
  public int hashCode() {
    return DO_HASHCODE_PRIME + ((id == null) ? 0 : id.hashCode());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof QuizAttemptDO)) {
      return false;
    }
    QuizAttemptDO other = (QuizAttemptDO) obj;
    if (id == null) {
      return other.id == null;
    } else {
      return id.equals(other.id);
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(final Long userId) {
    this.userId = userId;
  }

  public String getQuizId() {
    return quizId;
  }

  public void setQuizId(final String quizId) {
    this.quizId = quizId;
  }

  @Nullable
  public Long getQuizAssignmentId() {
    return quizAssignmentId;
  }

  public void setQuizAssignmentId(@Nullable final Long quizAssignmentId) {
    this.quizAssignmentId = quizAssignmentId;
  }

  public Instant getStartDate() {
    return startDate;
  }

  public void setStartDate(final Instant startDate) {
    this.startDate = startDate;
  }

  @Nullable
  public Instant getCompletedDate() {
    return completedDate;
  }

  public void setCompletedDate(@Nullable final Instant completedDate) {
    this.completedDate = completedDate;
  }
}
