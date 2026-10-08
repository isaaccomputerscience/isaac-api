package uk.ac.cam.cl.dtg.segue.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventNotificationEmailManager;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.ExpiredReservation;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.PgEventBookings;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.ExpiredReservationsCleanUpJob;

/**
 * Runs the real Quartz scheduler and event_bookings SQL against PostgreSQL (no Elasticsearch needed).
 */
class ExpiredReservationsJobIT {
  private static final String LEGACY_JOB = "cleanUpExpiredReservations";
  private static final String NEW_JOB = "cleanUpExpiredReservationsAndNotify";

  private static PostgreSQLContainer<?> postgres;
  private static PostgresSqlDb db;
  private static Injector originalInjector;
  private static final CountDownLatch jobRan = new CountDownLatch(1);

  @BeforeAll
  static void setUp() throws Exception {
    postgres = new PostgreSQLContainer<>("postgres:14-alpine")
        .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
        .withUsername("rutherford")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("db_scripts/postgres-rutherford-create-script.sql"),
            "/docker-entrypoint-initdb.d/00-isaac-create.sql")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("db_scripts/postgres-rutherford-functions.sql"),
            "/docker-entrypoint-initdb.d/01-isaac-functions.sql")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("db_scripts/quartz_scheduler_create_script.sql"),
            "/docker-entrypoint-initdb.d/02-isaac-quartz.sql");
    postgres.start();
    db = new PostgresSqlDb(postgres.getJdbcUrl(), "rutherford", "irrelevant");

    // Quartz jobs find their dependencies through the static Guice injector, so swap in a minimal one.
    EventNotificationEmailManager notifier = new EventNotificationEmailManager(null, null, null, null, null) {
      @Override
      public void cancelExpiredReservations() {
        jobRan.countDown();
      }
    };
    Field injectorField = SegueGuiceConfigurationModule.class.getDeclaredField("injector");
    injectorField.setAccessible(true);
    originalInjector = (Injector) injectorField.get(null);
    injectorField.set(null, Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(PostgresSqlDb.class).toInstance(db);
        bind(EventNotificationEmailManager.class).toInstance(notifier);
      }
    }));
  }

  @AfterAll
  static void tearDown() throws Exception {
    Field injectorField = SegueGuiceConfigurationModule.class.getDeclaredField("injector");
    injectorField.setAccessible(true);
    injectorField.set(null, originalInjector);
    postgres.stop();
  }

  private static int count(final String sql) throws SQLException {
    try (Connection conn = db.getDatabaseConnection();
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
      rs.next();
      return rs.getInt(1);
    }
  }

  @Test
  void legacySqlJobIsReplacedByNotifyingJobAndFires() throws Exception {
    SegueScheduledJob legacy = new SegueScheduledDatabaseScriptJob(LEGACY_JOB, "SQLMaintenance", "legacy",
        "0 0 7 * * ?", "db_scripts/scheduled/expired-reservations-clean-up.sql");
    SegueScheduledJob replacement = SegueScheduledJob.createCustomJob(NEW_JOB, "JavaJob", "replacement",
        "0 0 7 * * ?", new HashMap<>(), new ExpiredReservationsCleanUpJob());

    // State of an existing deployment: only the legacy job is registered.
    SegueJobService beforeDeploy = new SegueJobService(db, new ArrayList<>(List.of(legacy)), new ArrayList<>());
    assertEquals(1, count("SELECT count(*) FROM quartz_cluster.qrtz_job_details WHERE job_name = '" + LEGACY_JOB
        + "'"));
    beforeDeploy.contextDestroyed(null);

    SegueJobService afterDeploy =
        new SegueJobService(db, new ArrayList<>(List.of(replacement)), new ArrayList<>(List.of(legacy)));
    try {
      assertEquals(0, count("SELECT count(*) FROM quartz_cluster.qrtz_job_details WHERE job_name = '" + LEGACY_JOB
          + "'"));
      assertEquals(0, count("SELECT count(*) FROM quartz_cluster.qrtz_triggers WHERE job_name = '" + LEGACY_JOB
          + "'"));
      assertEquals(1, count("SELECT count(*) FROM quartz_cluster.qrtz_job_details WHERE job_name = '" + NEW_JOB
          + "'"));
      assertEquals(1, count("SELECT count(*) FROM quartz_cluster.qrtz_triggers WHERE job_name = '" + NEW_JOB + "'"));

      Scheduler scheduler = new StdSchedulerFactory().getScheduler("SegueScheduler");
      scheduler.triggerJob(new JobKey(NEW_JOB, "JavaJob"));
      assertTrue(jobRan.await(15, TimeUnit.SECONDS), "Job did not run when triggered through the scheduler");
    } finally {
      afterDeploy.contextDestroyed(null);
    }
  }

  @Test
  void cancelExpiredReservationsOnlyCancelsLapsedReservations() throws Exception {
    try (Connection conn = db.getDatabaseConnection(); Statement st = conn.createStatement()) {
      st.execute("INSERT INTO users(id, email, role) VALUES (1, 't@example.com', 'TEACHER'),"
          + " (2, 's1@example.com', 'STUDENT'), (3, 's2@example.com', 'STUDENT'), (4, 's3@example.com', 'STUDENT')");
      st.execute("INSERT INTO event_bookings(event_id, created, user_id, reserved_by, status,"
          + " additional_booking_information) VALUES"
          + " ('e1', now(), 2, 1, 'RESERVED', '{\"reservationCloseDate\":\"2020-01-01T07:00:00.000Z\"}'),"
          + " ('e1', now(), 3, 1, 'RESERVED', '{\"reservationCloseDate\":\"2999-01-01T07:00:00.000Z\"}'),"
          + " ('e1', now(), 4, 1, 'CONFIRMED', null)");
    }
    PgEventBookings bookings = new PgEventBookings(db, new ObjectMapper());

    List<ExpiredReservation> expired = bookings.cancelExpiredReservations();

    assertEquals(1, expired.size());
    ExpiredReservation reservation = expired.get(0);
    assertEquals("e1", reservation.eventId());
    assertEquals(2L, reservation.userId());
    assertEquals(1L, reservation.reservedById());
    assertEquals(1, count("SELECT count(*) FROM event_bookings WHERE status = 'CANCELLED'"));
    assertEquals(1, count("SELECT count(*) FROM event_bookings WHERE status = 'RESERVED'"));
    assertEquals(1, count("SELECT count(*) FROM event_bookings WHERE status = 'CONFIRMED'"));
    assertTrue(bookings.cancelExpiredReservations().isEmpty(), "A reservation must only be returned once");
  }
}
