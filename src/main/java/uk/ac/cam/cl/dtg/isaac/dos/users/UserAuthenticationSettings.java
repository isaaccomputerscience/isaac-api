/**
 * Copyright 2019 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.dos.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import uk.ac.cam.cl.dtg.isaac.mappers.Default;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;

/**
 * Immutable (virtual) Data Object to represent the user authentication settings.
 */
public class UserAuthenticationSettings extends AbstractSegueUser {
  private Long id;

  private List<AuthenticationProvider> linkedAccounts;
  private boolean hasSegueAccount;

  private boolean mfaStatus;

  /**
   * Create a UserAuthenticationSettings DTO.
   *
   * @param id              - user id
   * @param linkedAccounts  - The list of linked accounts the user has setup
   * @param hasSegueAccount - boolean whether or not they have a segue account
   * @param mfaStatus       - indicates if MFA is enabled for the account.
   */
  // As several properties on this class lack a setter, MapStruct must be directed to use this constructor instead of
  // generating an empty object and applying setters
  @Default
  public UserAuthenticationSettings(final Long id, final List<AuthenticationProvider> linkedAccounts,
                                    final boolean hasSegueAccount, final boolean mfaStatus) {
    this.id = id;
    this.linkedAccounts = linkedAccounts;
    this.hasSegueAccount = hasSegueAccount;
    this.mfaStatus = mfaStatus;
  }

  /**
   * Default constructor required for Jackson.
   */
  public UserAuthenticationSettings() {

  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  /**
   * Gets the linkedAccounts.
   *
   * @return the linkedAccounts
   */
  public List<AuthenticationProvider> getLinkedAccounts() {
    return linkedAccounts;
  }

  /**
   * Gets the hasSegueAccount.
   *
   * @return the hasSegueAccount
   */
  public boolean getHasSegueAccount() {
    return hasSegueAccount;
  }

  /**
   * Gets if MFA is enabled for the account.
   *
   * @return Enabled is true disabled is false
   */
  public boolean isMfaStatus() {
    return mfaStatus;
  }

  /**
   * Gets if MFA is enabled for the account.
   *
   * @param mfaStatus boolean
   */
  public void setMfaStatus(final boolean mfaStatus) {
    this.mfaStatus = mfaStatus;
  }
}
