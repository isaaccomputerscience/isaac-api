package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for returning project titles for a competition.
 */
public class ProjectTitlesResponseDTO {
  private List<String> projectTitles;

  /**
   * Default constructor for Jackson.
   */
  public ProjectTitlesResponseDTO() {
    this.projectTitles = new ArrayList<>();
  }

  /**
   * Constructor with project titles.
   *
   * @param projectTitles the list of project titles
   */
  public ProjectTitlesResponseDTO(List<String> projectTitles) {
    this.projectTitles = projectTitles != null ? projectTitles : new ArrayList<>();
  }

  /**
   * Gets the project titles.
   *
   * @return the list of project titles
   */
  @JsonProperty("projectTitles")
  public List<String> getProjectTitles() {
    return projectTitles;
  }

  /**
   * Sets the project titles.
   *
   * @param projectTitles the list of project titles to set
   */
  @JsonProperty("projectTitles")
  public void setProjectTitles(List<String> projectTitles) {
    this.projectTitles = projectTitles;
  }

  @Override
  public String toString() {
    return "ProjectTitlesResponseDTO{" +
        "projectTitles=" + projectTitles +
        '}';
  }
}
