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

package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import java.time.Instant;
import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

/**
 * Event Booking object.
 *
 * @author sac92
 *
 */
public class CompetitionEntryDTO {

  List<Long> entrantIds;
  String submissionURL;
  String groupName;

  /**
   * CompetitionEntryDTO.
   */
  public CompetitionEntryDTO() {

  }

  /**
   * Constructor for CompetitionEntryDTO.
   *
   * @param entrantIds
   *            - list of the userIds of the entrants
   * @param submissionURL
   *            - the URL of the entry submission
   */
  public CompetitionEntryDTO(final List<Long> entrantIds, final String submissionURL) {
    this.entrantIds = entrantIds;
    this.submissionURL = submissionURL;
  }

  public List<Long> getEntrantIds() {
    return entrantIds;
  }

  public void setEntrantIds(List<Long> entrantIds) {
    this.entrantIds = entrantIds;
  }

  public String getSubmissionURL() {
    return submissionURL;
  }

  public void setSubmissionURL(String submissionURL) {
    this.submissionURL = submissionURL;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }
}
