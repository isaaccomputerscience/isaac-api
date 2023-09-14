package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

public class QuizAttemptFeedbackDTO {
  private UserSummaryDTO user;
  private QuizAttemptDTO attempt;

  public QuizAttemptFeedbackDTO(final UserSummaryDTO user, final QuizAttemptDTO attempt) {
    this.user = user;
    this.attempt = attempt;
  }

  public UserSummaryDTO getUser() {
    return user;
  }

  public void setUser(final UserSummaryDTO user) {
    this.user = user;
  }

  public QuizAttemptDTO getAttempt() {
    return attempt;
  }

  public void setAttempt(final QuizAttemptDTO attempt) {
    this.attempt = attempt;
  }
}
