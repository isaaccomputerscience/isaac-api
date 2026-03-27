package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Legacy wrapper for backwards compatibility with existing Quartz scheduler database entries.
 * This job delegates to ExpiredReservationsCleanUpJob which contains the actual implementation.
 *
 * This allows us to keep the existing trigger name in the database while using the new
 * ExpiredReservationsCleanUpJob implementation.
 */
public class CancelExpiredReservationsJob implements Job {

  private final ExpiredReservationsCleanUpJob delegate = new ExpiredReservationsCleanUpJob();

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    delegate.execute(context);
  }
}
