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

package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.time.Instant;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;

/**
 * Notification. This class represents a special content type which can be used to pop up a modal to users on the site.
 *
 * @author sac92
 */
public class NotificationDTO extends ContentDTO {
  private ExternalReference externalReference;
  private Instant expiry;

  /**
   * Notification object constructor taking a string value for the notification content.
   *
   * @param value for the value field
   */
  public NotificationDTO(final String value) {
    super(value);
  }

  /**
   * Notification object constructor that does not assign any initial value for the notification content.
   */
  public NotificationDTO() {

  }

  /**
   * Gets the externalReference.
   *
   * @return the externalReference
   */
  public ExternalReference getExternalReference() {
    return externalReference;
  }

  /**
   * Sets the externalReference.
   *
   * @param externalReference the externalReference to set
   */
  public void setExternalReference(final ExternalReference externalReference) {
    this.externalReference = externalReference;
  }

  /**
   * Gets the expiry.
   *
   * @return the expiry
   */
  public Instant getExpiry() {
    return expiry;
  }

  /**
   * Sets the expiry.
   *
   * @param expiry the expiry to set
   */
  public void setExpiry(final Instant expiry) {
    this.expiry = expiry;
  }
}
