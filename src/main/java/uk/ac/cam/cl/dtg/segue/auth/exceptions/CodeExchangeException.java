/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception to indicate that there was a problem exchanging code during a 3rd party authentication process.
 */
public class CodeExchangeException extends Exception {
  private static final long serialVersionUID = 4138638995944518854L;

  /**
   * Default Constructor.
   */
  public CodeExchangeException() {
    super();
  }

  /**
   * Constructor with message.
   *
   * @param message to explain exception.
   */
  public CodeExchangeException(final String message) {
    super(message);
  }

  /**
   * Constructor with message and cause.
   *
   * @param message explaining the exception
   * @param cause if there is a root cause.
   */
  public CodeExchangeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
