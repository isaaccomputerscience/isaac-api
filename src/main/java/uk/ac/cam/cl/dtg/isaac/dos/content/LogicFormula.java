/**
 * Copyright 2019 James Sharkey
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

package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.LogicFormulaDTO;

/**
 * LogicFormula is a specialised choice object that allows a python expression representing the formula to be stored.
 */
@DTOMapping(LogicFormulaDTO.class)
@JsonContentType("logicFormula")
public class LogicFormula extends Choice {
  private String pythonExpression;
  private boolean requiresExactMatch;

  public LogicFormula() {

  }

  public String getPythonExpression() {
    return pythonExpression;
  }

  public void setPythonExpression(final String pythonExpression) {
    this.pythonExpression = pythonExpression;
  }

  public boolean getRequiresExactMatch() {
    return requiresExactMatch;
  }

  public void setRequiresExactMatch(final boolean requiresExactMatch) {
    this.requiresExactMatch = requiresExactMatch;
  }
}
