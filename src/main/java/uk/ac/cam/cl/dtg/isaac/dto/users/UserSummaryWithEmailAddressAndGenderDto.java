/**
 * Copyright 2023 Kasim Ali
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

package uk.ac.cam.cl.dtg.isaac.dto.users;

/**
 * View of a User Summary object, which contains additional information (email and gender).
 * Usually used where a sharing relationship has or will be created.
 */
public class UserSummaryWithEmailAddressAndGenderDto extends UserSummaryWithEmailAddressDTO {
  private String gender;

  /**
   * Gets the gender.
   *
   * @return the gender
   */
  public String getGender() {
    return gender;
  }


  /**
   * Sets the gender.
   *
   * @param gender - The gender of the user
   */
  public void setGender(final String gender) {
    this.gender = gender;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("UserSummaryDTO [id=");
    builder.append(this.getId());
    builder.append(", givenName=");
    builder.append(this.getGivenName());
    builder.append(", familyName=");
    builder.append(this.getFamilyName());
    builder.append(", email=");
    builder.append(this.getEmail());
    builder.append(", gender=");
    builder.append(gender);
    builder.append(", emailVerificationStatus=");
    builder.append(this.getEmailVerificationStatus().name());
    builder.append(", authorisedFullAccess=");
    builder.append(this.isAuthorisedFullAccess());
    builder.append("]");
    return builder.toString();
  }
}
