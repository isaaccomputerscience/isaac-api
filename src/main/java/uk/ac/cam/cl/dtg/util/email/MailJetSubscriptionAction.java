/**
 * Copyright 2021 James Sharkey
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

package uk.ac.cam.cl.dtg.util.email;

public enum MailJetSubscriptionAction {
  SOFT_SUBSCRIBE("addnoforce"),
  FORCE_SUBSCRIBE("addforce"),
  UNSUBSCRIBE("unsub"),
  REMOVE("remove");

  private final String value;

  /**
   * Class to represent the different possible subscription actions supported by the MailJet API.
   *
   * @param value string value needed by MailJet
   */
  MailJetSubscriptionAction(final String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
