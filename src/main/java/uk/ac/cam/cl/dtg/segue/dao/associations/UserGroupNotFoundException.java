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

package uk.ac.cam.cl.dtg.segue.dao.associations;

/**
 * UserAssociationException. This exception means that an account already exists with an key field.
 *
 */
public class UserGroupNotFoundException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Create a new DuplicateAccountException.
   *
   * @param message
   *            - message to add
   */
  public UserGroupNotFoundException(final String message) {
    super(message);
  }

}
