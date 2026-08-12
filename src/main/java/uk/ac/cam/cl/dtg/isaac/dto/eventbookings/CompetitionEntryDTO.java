package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import java.util.List;

/**
 * Competition Booking object.
 *
 * @author cwc
 *
 */
public class CompetitionEntryDTO {

  private List<Long> entrantIds;
  private String submissionURL;
  private String groupName;
  private String projectTitle;
  private String yearGroup;
  private String projectDescription;

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

  public String getProjectTitle() {
    return projectTitle;
  }

  public void setProjectTitle(String projectTitle) {
    this.projectTitle = projectTitle;
  }

  public String getYearGroup() {
    return yearGroup;
  }

  public void setYearGroup(String yearGroup) {
    this.yearGroup = yearGroup;
  }

  public String getProjectDescription() {
    return projectDescription;
  }

  public void setProjectDescription(String projectDescription) {
    this.projectDescription = projectDescription;
  }
}
