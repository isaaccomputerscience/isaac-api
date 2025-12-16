package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO for returning project titles for a competition.
 */
public class ProjectTitlesResponseDTO {
  private Set<String> projectTitles;

  /**
   * Default constructor for Jackson.
   */
  public ProjectTitlesResponseDTO() {
    this.projectTitles = new HashSet<>();
  }

  /**
   * Constructor with project titles.
   *
   * @param projectTitles the list of project titles
   */
  public ProjectTitlesResponseDTO(Set<String> projectTitles) {
    this.projectTitles = projectTitles != null ? projectTitles : new HashSet<>();
  }

  /**
   * Gets the project titles.
   *
   * @return the list of project titles
   */
  @JsonProperty("projectTitles")
  public Set<String> getProjectTitles() {
    return projectTitles;
  }

  /**
   * Sets the project titles.
   *
   * @param projectTitles the list of project titles to set
   */
  @JsonProperty("projectTitles")
  public void setProjectTitles(Set<String> projectTitles) {
    this.projectTitles = projectTitles;
  }

  @Override
  public String toString() {
    return "ProjectTitlesResponseDTO{"
        + "projectTitles="
        + projectTitles
        + '}';
  }
}
