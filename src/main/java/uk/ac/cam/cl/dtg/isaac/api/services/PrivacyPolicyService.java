package uk.ac.cam.cl.dtg.isaac.api.services;

import com.google.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.InvalidTimestampException;
import uk.ac.cam.cl.dtg.isaac.api.requests.PrivacyPolicyRequest;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public class PrivacyPolicyService {

  private static final Logger log = LoggerFactory.getLogger(PrivacyPolicyService.class);
  private static final long TIME_TOLERANCE_SECONDS = 30;

  private final UserAccountManager userManager;
  private final Clock clock;

  @Inject
  public PrivacyPolicyService(UserAccountManager userManager, Clock clock) {
    this.userManager = userManager;
    this.clock = clock;
  }

  /**
   * Accept privacy policy with timestamp validation
   * @param request HTTP request to get current user
   * @param privacyPolicyRequest Request containing the timestamp
   * @throws NoUserLoggedInException if no user is logged in
   * @throws SegueDatabaseException if database error occurs
   * @throws InvalidTimestampException if timestamp is too far from current time
   */
  public void acceptPrivacyPolicy(HttpServletRequest request, PrivacyPolicyRequest privacyPolicyRequest)
      throws NoUserLoggedInException, SegueDatabaseException, InvalidTimestampException {

    // Get current user
    RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

    // Validate timestamp
    Instant providedTime = privacyPolicyRequest.getPrivacyPolicyAcceptedTimeInstant();
    validateTimestamp(providedTime);

    // Update privacy policy acceptance
    userManager.updatePrivacyPolicyAcceptedTime(user, providedTime);

    log.info("User {} accepted privacy policy at {}", user.getEmail(), providedTime);
  }

  /**
   * Validates that the provided timestamp is close to the current time
   * @param providedTime The timestamp from the client
   * @throws InvalidTimestampException if timestamp is outside allowed tolerance
   */
  private void validateTimestamp(Instant providedTime) throws InvalidTimestampException {
    Instant currentTime = clock.instant();
    long secondsDifference = Math.abs(Duration.between(providedTime, currentTime).getSeconds());

    if (secondsDifference > TIME_TOLERANCE_SECONDS) {
      throw new InvalidTimestampException(
          String.format("Timestamp too far from current time. Difference: %d seconds, Max allowed: %d seconds",
              secondsDifference, TIME_TOLERANCE_SECONDS)
      );
    }
  }
}