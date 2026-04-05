package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public class CancelExpiredReservationsJob implements Job {
  private static final Logger log = LoggerFactory.getLogger(CancelExpiredReservationsJob.class);
  private final EventBookingManager eventBookingManager;

  /**
   * This class is required by quartz and must be executable by any instance of the segue api relying only on the
   * jobdata context provided.
   */
  public CancelExpiredReservationsJob() {
    Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
    eventBookingManager = injector.getInstance(EventBookingManager.class);
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    try {
      eventBookingManager.cancelExpiredReservations();
      log.info("Ran CancelExpiredReservationsJob successfully");
    } catch (SegueDatabaseException e) {
      log.error("Error running CancelExpiredReservationsJob", e);
      throw new JobExecutionException("Failed to cancel expired reservations", e);
    }
  }
}
