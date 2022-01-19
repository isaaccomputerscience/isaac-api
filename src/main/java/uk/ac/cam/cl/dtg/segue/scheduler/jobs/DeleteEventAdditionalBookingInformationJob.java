package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.api.client.util.Maps;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.ExternalAccountSynchronisationException;
import uk.ac.cam.cl.dtg.segue.api.managers.IExternalAccountManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.ENDDATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class DeleteEventAdditionalBookingInformationJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(DeleteEventAdditionalBookingInformationJob.class);

    private final PropertiesLoader properties;
    private final PostgresSqlDb database;
    private final IContentManager contentManager;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public DeleteEventAdditionalBookingInformationJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        properties = injector.getInstance(PropertiesLoader.class);
        contentManager = injector.getInstance(IContentManager.class);
        database = injector.getInstance(PostgresSqlDb.class);

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Integer limit = 10000;
        Integer startIndex = 0;
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));
        sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);
        DateRangeFilterInstruction anyEventsToNow = new DateRangeFilterInstruction(null, new Date());
        filterInstructions.put(ENDDATE_FIELDNAME, anyEventsToNow);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime thirtyDaysAgo = now.plusDays(-30);
        try {
            ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
                    properties.getProperty(CONTENT_INDEX), ContentService.generateDefaultFieldToMatch(fieldsToMatch),
                    startIndex, limit, sortInstructions, filterInstructions);
            for (ContentDTO contentResult : findByFieldNames.getResults()) {
                if (contentResult instanceof IsaacEventPageDTO) {
                    IsaacEventPageDTO page = (IsaacEventPageDTO) contentResult;
                    // Event end date (if present) > 30 days ago, else event date > 30 days ago
                    if ((page.getEndDate() != null && page.getEndDate().toInstant().isBefore(thirtyDaysAgo.toInstant())) || (page.getDate().toInstant().isBefore(thirtyDaysAgo.toInstant()))) {
                        try (Connection conn = database.getDatabaseConnection()) {
                            PreparedStatement pst;
                            pst = conn
                                    .prepareStatement("UPDATE event_bookings SET additional_booking_information=jsonb_set(jsonb_set(jsonb_set(jsonb_set(" +
                                            "additional_booking_information," +
                                            " '{emergencyName}', '\"[REMOVED]\"'::JSONB, FALSE)," +
                                            " '{emergencyNumber}', '\"[REMOVED]\"'::JSONB, FALSE)," +
                                            " '{accessibilityRequirements}', '\"[REMOVED]\"'::JSONB, FALSE)," +
                                            " '{medicalRequirements}', '\"[REMOVED]\"'::JSONB, FALSE)" +
                                            " WHERE event_id = ?" +
                                            " AND additional_booking_information ??| array['emergencyName', 'emergencyNumber', 'accessibilityRequirements', 'medicalRequirements'];");
                            pst.setString(1, page.getId());

                            int affectedRows = pst.executeUpdate();
                            if (affectedRows > 0) {
                                log.info("Event " + page.getId() + " had " + affectedRows + " bookings which have been scrubbed of PII");
                            }
                        }
                    }
                }
            }
            log.info("Ran DeleteEventAdditionalBookingInformationJob");
        } catch (ContentManagerException e) {
            log.error("Failed to delete event additional booking information");
            e.printStackTrace();
        } catch (SQLException e) {
            try {
                throw new SegueDatabaseException("Postgres exception", e);
            } catch (SegueDatabaseException ex) {
                ex.printStackTrace();
            }
        } catch (Error e) {
            log.error("Failed to delete event additional booking information");
            e.printStackTrace();
        }

    }
}
