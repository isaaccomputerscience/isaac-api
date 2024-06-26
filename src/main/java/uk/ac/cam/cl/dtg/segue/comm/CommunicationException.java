/**
 * Copyright 2014 Nick Rogers
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

package uk.ac.cam.cl.dtg.segue.comm;

/**
 * An exception whilst trying to complete a communication action.
 *
 * @author nr378
 */
public class CommunicationException extends Exception {
  private static final long serialVersionUID = 7797028995535006701L;

  /**
   * Exception constructor.
   *
   * @param e parent exception
   */
  public CommunicationException(final Exception e) {
    super(e);
  }
}
