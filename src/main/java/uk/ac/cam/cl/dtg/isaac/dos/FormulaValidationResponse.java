/**
 * Copyright 2016 Ian Davies
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

import java.util.Date;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dto.FormulaValidationResponseDTO;

/**
 * The DO which can be used to inform clients of the result of an answered question.
 *
 */
@DTOMapping(FormulaValidationResponseDTO.class)
public class FormulaValidationResponse extends QuestionValidationResponse {
  private String matchType;

  /**
   * Default constructor.
   */
  public FormulaValidationResponse() {}

  /**
   * Full constructor.
   *
   * @param questionId
   *            -
   * @param answer
   *            -
   * @param explanation
   *            -
   * @param correct
   *            -
   * @param matchType
   *            -
   * @param dateAttempted
   *            -
   */
  public FormulaValidationResponse(
    final String questionId,
    final Choice answer,
    final Content explanation,
    final Boolean correct,
    final String matchType,
    final Date dateAttempted
  ) {
    super(questionId, answer, correct, explanation, dateAttempted);
    this.matchType = matchType;
  }

  /**
   * Gets the matchType.
   *
   * @return the matchType
   */
  public final String matchType() {
    return matchType;
  }

  /**
   * Sets the matchType.
   *
   * @param matchType
   *            the matchType to set
   */
  public final void matchType(final String matchType) {
    this.matchType = matchType;
  }

  @Override
  public String toString() {
    return "FormulaValidationResponse [correct=" + this.isCorrect() + ",matchType=" + this.matchType + "]";
  }
}
