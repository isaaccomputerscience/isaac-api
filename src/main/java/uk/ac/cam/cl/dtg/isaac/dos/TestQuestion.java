/**
 * Copyright 2020 Meurig Thomas
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

import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;

/**
 * Data structure to hold the choices of a fake question and the test cases to test it with.
 */
public class TestQuestion {
  private List<Choice> userDefinedChoices;
  private List<TestCase> testCases;

  public void setUserDefinedChoices(final List<Choice> userDefinedChoices) {
    this.userDefinedChoices = userDefinedChoices;
  }

  public List<Choice> getUserDefinedChoices() {
    return this.userDefinedChoices;
  }

  public void setTestCases(final List<TestCase> tests) {
    this.testCases = tests;
  }

  public List<TestCase> getTestCases() {
    return this.testCases;
  }
}