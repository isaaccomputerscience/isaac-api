package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventNotificationEmailManager;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;

public class ExpiredReservationsCleanUpJob implements Job {
  private static final Logger log = LoggerFactory.getLogger(ExpiredReservationsCleanUpJob.class);
  private final EventNotificationEmailManager eventNotificationEmailManager;

  /**
   * This class is required by quartz and must be executable by any instance of the segue api relying only on the
   * jobdata context provided.
   */
  public ExpiredReservationsCleanUpJob() {
    Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
    eventNotificationEmailManager = injector.getInstance(EventNotificationEmailManager.class);
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    eventNotificationEmailManager.cancelExpiredReservations();
    log.info("Ran ExpiredReservationsCleanUpJob");
  }
}
