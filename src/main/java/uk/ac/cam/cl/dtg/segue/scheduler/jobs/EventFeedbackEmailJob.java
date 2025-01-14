package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.inject.Injector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventNotificationEmailManager;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;



public class EventFeedbackEmailJob implements Job {
  private static final Logger log = LoggerFactory.getLogger(EventFeedbackEmailJob.class);
  private final EventNotificationEmailManager scheduledEmailManager;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  /**
   * This class is required by quartz and must be executable by any instance of the segue api relying only on the
   * jobdata context provided.
   */
  public EventFeedbackEmailJob() {
    Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
    scheduledEmailManager = injector.getInstance(EventNotificationEmailManager.class);
  }

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    scheduler.schedule(() -> {
      try {
        scheduledEmailManager.sendFeedbackEmails();
        log.info("Ran EventFeedbackEmailJob after 24 hours delay");
      } catch (Exception e) {
        log.error("Failed to send feedback emails", e);
      }
    }, 24, TimeUnit.HOURS);
  }
}