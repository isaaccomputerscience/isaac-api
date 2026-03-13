package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.inject.Injector;
import java.util.List;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.IUserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

/**
 * Scheduled job to clean up expired RESERVED event bookings and trigger auto-promotion of waiting list users.
 * This job replaces the simple SQL cleanup with proper auto-promotion logic by:
 * 1. Finding all RESERVED bookings that have passed their reservationCloseDate
 * 2. For each expired reservation, calling cancelBooking() to trigger auto-promotion
 * 3. Promoting waiting list users in chronological order as spots become available
 * Previously, the SQL-only approach would cancel reservations but not promote waiting list users,
 * leaving events marked as full of available spots.
 */
public class ExpiredReservationsCleanUpJob implements Job {
  private static final Logger log = LoggerFactory.getLogger(ExpiredReservationsCleanUpJob.class);

  private final EventBookingPersistenceManager bookingPersistenceManager;
  private final EventBookingManager bookingManager;
  private final IUserAccountManager userAccountManager;
  private final GitContentManager contentManager;

  /**
   * Constructor for Quartz Job - must be no-arg and retrieve dependencies from injector.
   */
  public ExpiredReservationsCleanUpJob() {
    Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
    bookingPersistenceManager = injector.getInstance(EventBookingPersistenceManager.class);
    bookingManager = injector.getInstance(EventBookingManager.class);
    userAccountManager = injector.getInstance(IUserAccountManager.class);
    contentManager = injector.getInstance(GitContentManager.class);
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    try {
      log.info("Starting ExpiredReservationsCleanUpJob");

      List<DetailedEventBookingDTO> expiredReservations =
          bookingPersistenceManager.getExpiredReservedBookings();

      if (expiredReservations.isEmpty()) {
        log.info("No expired reservations found");
        return;
      }

      log.info("Found {} expired RESERVED bookings to process", expiredReservations.size());

      int successCount = 0;
      int failureCount = 0;

      for (DetailedEventBookingDTO expiredBooking : expiredReservations) {
        try {
          // Get the event details
          IsaacEventPageDTO event = (IsaacEventPageDTO) contentManager
              .getContentById(expiredBooking.getEventId());

          if (event == null) {
            log.warn("Event {} not found for expired booking {}. Skipping.",
                expiredBooking.getEventId(), expiredBooking.getBookingId());
            failureCount++;
            continue;
          }

          // Get the user details
          RegisteredUserDTO user = userAccountManager
              .getUserDTOById(expiredBooking.getUserBooked().getId());

          if (user == null) {
            log.warn("User {} not found for expired booking {}. Skipping.",
                expiredBooking.getUserBooked().getId(), expiredBooking.getBookingId());
            failureCount++;
            continue;
          }

          // Cancel the booking - this triggers auto-promotion of waiting list users!
          try {
            bookingManager.cancelBooking(event, user);
            log.info("Cancelled expired RESERVED booking for user {} on event {} (booking id: {})",
                user.getId(), event.getId(), expiredBooking.getBookingId());
            successCount++;
          } catch (SegueDatabaseException e) {
            log.error("Database error while cancelling expired reservation {} for user {} on event {}",
                expiredBooking.getBookingId(), user.getId(), event.getId(), e);
            failureCount++;
          } catch (ContentManagerException e) {
            log.error("Content manager error while cancelling expired reservation {} for user {} on event {}",
                expiredBooking.getBookingId(), user.getId(), event.getId(), e);
            failureCount++;
          }
        } catch (NoUserException e) {
          log.warn("User not found for expired booking {}", expiredBooking.getBookingId(), e);
          failureCount++;
        } catch (ResourceNotFoundException e) {
          log.warn("Event not found for expired booking {}", expiredBooking.getBookingId(), e);
          failureCount++;
        } catch (ContentManagerException e) {
          log.error("Content manager error while retrieving event for expired booking {}",
              expiredBooking.getBookingId(), e);
          failureCount++;
        }
      }

      log.info("ExpiredReservationsCleanUpJob completed: {} successful cancellations, {} failures",
          successCount, failureCount);

    } catch (SegueDatabaseException e) {
      log.error("Failed to retrieve expired reservations for ExpiredReservationsCleanUpJob", e);
      throw new JobExecutionException(e);
    }
  }
}
