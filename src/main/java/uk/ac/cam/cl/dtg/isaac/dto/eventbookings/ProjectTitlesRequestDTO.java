package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for requesting project titles for a competition.
 */
public class ProjectTitlesRequestDTO {
  private String competitionId;
  private List<Long> userIds;

  /**
   * Default constructor for Jackson.
   */
  public ProjectTitlesRequestDTO() {
  }

  /**
   * Constructor with fields.
   *
   * @param competitionId the competition/event ID
   * @param userIds list of user IDs to get project titles for
   */
  public ProjectTitlesRequestDTO(String competitionId, List<Long> userIds) {
    this.competitionId = competitionId;
    this.userIds = userIds;
  }

  /**
   * Gets the competition ID.
   *
   * @return the competition ID
   */
  @JsonProperty("competitionId")
  public String getCompetitionId() {
    return competitionId;
  }

  /**
   * Sets the competition ID.
   *
   * @param competitionId the competition ID to set
   */
  @JsonProperty("competitionId")
  public void setCompetitionId(String competitionId) {
    this.competitionId = competitionId;
  }

  /**
   * Gets the user IDs.
   *
   * @return the list of user IDs
   */
  @JsonProperty("userIds")
  public List<Long> getUserIds() {
    return userIds;
  }

  /**
   * Sets the user IDs.
   *
   * @param userIds the list of user IDs to set
   */
  @JsonProperty("userIds")
  public void setUserIds(List<Long> userIds) {
    this.userIds = userIds;
  }

  @Override
  public String toString() {
    return "ProjectTitlesRequestDTO{" +
        "competitionId='" + competitionId + '\'' +
        ", userIds=" + userIds +
        '}';
  }
}
