/**
 * Copyright 2015 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.dao;

/**
 * The object requested does not exist.
 */
public class ResourceNotFoundException extends SegueDatabaseException {

  private static final long serialVersionUID = 8735657977878491171L;

  /**
   * Exception constructor with message for a resource that cannot be found.
   *
   * @param message explaining the issue.
   */
  public ResourceNotFoundException(final String message) {
    super(message);
  }

  /**
   * Exception constructor with message and wrapped exception for a resource that cannot be found.
   *
   * @param message explaining the issue.
   * @param e exception to wrap.
   */
  public ResourceNotFoundException(final String message, final Exception e) {
    super(message, e);
  }

}
