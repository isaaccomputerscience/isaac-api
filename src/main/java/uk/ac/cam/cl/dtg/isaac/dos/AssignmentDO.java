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

package uk.ac.cam.cl.dtg.isaac.dos;

import java.time.Instant;

/**
 * This class is the Domain Object used to store Assignments in the isaac CMS.
 */
public class AssignmentDO {
  private Long id;
  private String gameboardId;
  private Long groupId;
  private Long ownerUserId;
  private String notes;
  private Instant creationDate;
  private Instant dueDate;
  private Instant scheduledStartDate;

  /**
   * Complete AssignmentDO constructor with all dependencies.
   *
   * @param id                 unique id for the gameboard
   * @param gameboardId        The gameboard to assign as homework.
   * @param ownerUserId        User id of the owner of the gameboard.
   * @param groupId            Group id who should be assigned the game board.
   * @param notes              any additional information added to the assignment.
   * @param creationDate       the date the assignment was created.
   * @param dueDate            the date the assignment is due to be completed by.
   * @param scheduledStartDate the date and time the assignment should have notification emails sent to its group
   *                                 (using Quartz)
   */
  public AssignmentDO(final Long id, final String gameboardId, final Long ownerUserId, final Long groupId,
                      final String notes, final Instant creationDate, final Instant dueDate,
                      final Instant scheduledStartDate) {
    this.id = id;
    this.gameboardId = gameboardId;
    this.ownerUserId = ownerUserId;
    this.groupId = groupId;
    this.notes = notes;
    this.creationDate = creationDate;
    this.dueDate = dueDate;
    this.scheduledStartDate = scheduledStartDate;
  }

  /**
   * Default constructor required for AutoMapping.
   */
  public AssignmentDO() {

  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the id to set
   */
  public void setId(final Long id) {
    this.id = id;
  }

  /**
   * Gets the gameboardId.
   *
   * @return the gameboardId
   */
  public String getGameboardId() {
    return gameboardId;
  }

  /**
   * Sets the gameboardId.
   *
   * @param gameboardId the gameboardId to set
   */
  public void setGameboardId(final String gameboardId) {
    this.gameboardId = gameboardId;
  }

  /**
   * Gets the groupId.
   *
   * @return the groupId
   */
  public Long getGroupId() {
    return groupId;
  }

  /**
   * Sets the groupId.
   *
   * @param groupId the groupId to set
   */
  public void setGroupId(final Long groupId) {
    this.groupId = groupId;
  }

  /**
   * Gets the ownerUserId.
   *
   * @return the ownerUserId
   */
  public Long getOwnerUserId() {
    return ownerUserId;
  }

  /**
   * Sets the ownerUserId.
   *
   * @param ownerUserId the ownerUserId to set
   */
  public void setOwnerUserId(final Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  /**
   * get notes to an assignment.
   *
   * @return notes - the notes
   */
  public String getNotes() {
    return notes;
  }

  /**
   * set notes to an assignment.
   *
   * @param notes - the notes
   */
  public void setNotes(final String notes) {
    this.notes = notes;
  }

  /**
   * Gets the creationDate.
   *
   * @return the creationDate
   */
  public Instant getCreationDate() {
    return creationDate;
  }

  /**
   * Sets the creationDate.
   *
   * @param creationDate the creationDate to set
   */
  public void setCreationDate(final Instant creationDate) {
    this.creationDate = creationDate;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AssignmentDO other)) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  /**
   * get the due date of the assignment.
   *
   * @return dueDate
   */
  public Instant getDueDate() {
    return dueDate;
  }

  /**
   * set the due date of an assignment.
   *
   * @param dueDate - date due
   */
  public void setDueDate(final Instant dueDate) {
    this.dueDate = dueDate;
  }


  /**
   * get the date of when the assignment should be displayed to users.
   *
   * @return scheduledStartDate
   */
  public Instant getScheduledStartDate() {
    return scheduledStartDate;
  }

  /**
   * set the date of when the assignment should be displayed to users.
   *
   * @param scheduledStartDate - the scheduled start date
   */
  public void setScheduledStartDate(final Instant scheduledStartDate) {
    this.scheduledStartDate = scheduledStartDate;
  }
}
