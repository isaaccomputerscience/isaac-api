/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.ISAAC_CLIENT_LOG_TYPES;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.ISAAC_SERVER_LOG_TYPES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_SERVER_LOG_TYPES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.Constants.IsaacClientLogType;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.LogEventMisuseHandler;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * LogEventFacade. This facade is responsible for allowing the front end to log arbitrary information in the log
 * database.
 */
@Path("/log")
@Tag(name = "/log")
public class LogEventFacade extends AbstractSegueFacade {
  private static final Logger log = LoggerFactory.getLogger(LogEventFacade.class);

  private static final String VIDEO_ENGAGEMENT_EVENT_TYPE = IsaacClientLogType.VIDEO_60_PERCENT_WATCHED.name();
  private static final String VIDEO_ID_FIELDNAME = "videoId";
  private static final String VIDEO_DURATION_SECONDS_FIELDNAME = "videoDurationSeconds";
  private static final String WATCHED_SECONDS_FIELDNAME = "watchedSeconds";
  private static final String WATCH_PERCENT_FIELDNAME = "watchPercent";
  private static final double VIDEO_WATCH_THRESHOLD = 0.6;

  private final IMisuseMonitor misuseMonitor;

  private final UserAccountManager userManager;

  /**
   * Injectable constructor.
   *
   * @param properties    the propertiesLoader.
   * @param logManager    - For logging interesting user events.
   * @param misuseMonitor - So we can deal with misuse to incoming logged events.
   * @param userManager   - So we can attribute log events to a given user.
   */
  @Inject
  public LogEventFacade(final PropertiesLoader properties, final ILogManager logManager,
                        final IMisuseMonitor misuseMonitor, final UserAccountManager userManager) {
    super(properties, logManager);
    this.misuseMonitor = misuseMonitor;
    this.userManager = userManager;
  }

  /**
   * Method to allow clients to log front-end specific behaviour in the database.
   *
   * @param httpRequest - to enable retrieval of session information.
   * @param eventJSON   - the event information to record as a json map {@code <String, String>}.
   * @return 200 for success or 400 for failure.
   */
  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Record a new log event from the current user.",
      description = "The 'type' field must be provided and must not be a reserved value.")
  public Response postLog(@Context final HttpServletRequest httpRequest, final Map<String, Object> eventJSON) {
    if (null == eventJSON || eventJSON.get(TYPE_FIELDNAME) == null) {
      log.error("Error during log operation, no event type specified. Event: " + sanitiseExternalLogValue(eventJSON));
      return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to record log message as the log has no "
          + TYPE_FIELDNAME + " property.").toResponse();
    }

    String eventType = (String) eventJSON.get(TYPE_FIELDNAME);

    // To maintain data integrity - don't allow the client to report events reserved for the server
    if (SEGUE_SERVER_LOG_TYPES.contains(eventType) || ISAAC_SERVER_LOG_TYPES.contains(eventType)) {
      return new SegueErrorResponse(Status.FORBIDDEN, "Unable to record log message, restricted '"
          + TYPE_FIELDNAME + "' value.").toResponse();
    }

    // Temporarily log log event types which are not included in our accepted list of client log types.
    // After a few weeks we should fail on the case where it is an unknown type.
    if (!ISAAC_CLIENT_LOG_TYPES.contains(eventType)) {
      log.error(String.format("Warning: Log Event '%s' is not included in ISAAC_CLIENT_LOG_TYPES",
          sanitiseExternalLogValue(eventType)));
    }

    try {
      // implement arbitrary log size limit.
      AbstractSegueUserDTO currentUser = userManager.getCurrentUser(httpRequest);
      String uid;
      if (currentUser instanceof AnonymousUserDTO) {
        uid = ((AnonymousUserDTO) currentUser).getSessionId();
      } else {
        uid = ((RegisteredUserDTO) currentUser).getId().toString();
      }

      // Rate-limit every request (including video engagement events) before doing any further per-event work.
      try {
        misuseMonitor.notifyEvent(uid, LogEventMisuseHandler.class.getSimpleName(), httpRequest.getContentLength());
      } catch (SegueResourceMisuseException e) {
        log.error(String.format("Logging Event Failed - log event requested (%s bytes) "
                + "and would exceed daily limit size limit (%s bytes) ", httpRequest.getContentLength(),
            Constants.MAX_LOG_REQUEST_BODY_SIZE_IN_BYTES));
        return SegueErrorResponse.getRateThrottledResponse(String.format(
            "Log event request (%s bytes) would exceed limit for this endpoint.",
            httpRequest.getContentLength()));
      }

      // The video engagement event requires a logged-in user, a validated payload and is deduplicated per video.
      if (VIDEO_ENGAGEMENT_EVENT_TYPE.equals(eventType)) {
        if (!(currentUser instanceof RegisteredUserDTO)) {
          return new SegueErrorResponse(Status.UNAUTHORIZED,
              "You must be logged in to record a " + VIDEO_ENGAGEMENT_EVENT_TYPE + " event.").toResponse();
        }

        SegueErrorResponse validationError = validateVideoEngagementEvent(eventJSON);
        if (validationError != null) {
          return validationError.toResponse();
        }

        String videoId = (String) eventJSON.get(VIDEO_ID_FIELDNAME);
        if (this.getLogManager().userHasLoggedEventWithDetail(uid, VIDEO_ENGAGEMENT_EVENT_TYPE,
            VIDEO_ID_FIELDNAME, videoId)) {
          // Already recorded for this user + video; treat as a successful no-op so the client need not special-case it.
          return Response.ok().cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        }
      }

      // remove the type information as we don't need it.
      eventJSON.remove(TYPE_FIELDNAME);

      this.getLogManager()
          .logExternalEvent(this.userManager.getCurrentUser(httpRequest), httpRequest, eventType, eventJSON);

      return Response.ok().cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
    } catch (SegueDatabaseException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Database error while looking up user information.", e);
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    }
  }

  /**
   * Validate the payload of a {@value #VIDEO_ENGAGEMENT_EVENT_TYPE} event.
   *
   * @param eventJSON the client supplied event details.
   * @return a {@link SegueErrorResponse} describing the first validation failure, or null if the payload is valid.
   */
  private SegueErrorResponse validateVideoEngagementEvent(final Map<String, Object> eventJSON) {
    Object videoIdValue = eventJSON.get(VIDEO_ID_FIELDNAME);
    if (!(videoIdValue instanceof String videoId) || videoId.isBlank()) {
      return videoEventValidationError("<missing>", "missing or empty " + VIDEO_ID_FIELDNAME);
    }

    Double videoDurationSeconds = getNumericField(eventJSON, VIDEO_DURATION_SECONDS_FIELDNAME);
    if (videoDurationSeconds == null || videoDurationSeconds <= 0) {
      return videoEventValidationError(videoId, VIDEO_DURATION_SECONDS_FIELDNAME + " must be greater than 0");
    }

    Double watchedSeconds = getNumericField(eventJSON, WATCHED_SECONDS_FIELDNAME);
    if (watchedSeconds == null || watchedSeconds < 0) {
      return videoEventValidationError(videoId, WATCHED_SECONDS_FIELDNAME + " must be greater than or equal to 0");
    }

    Double watchPercent = getNumericField(eventJSON, WATCH_PERCENT_FIELDNAME);
    if (watchPercent == null || watchPercent < VIDEO_WATCH_THRESHOLD) {
      return videoEventValidationError(videoId,
          WATCH_PERCENT_FIELDNAME + " must be greater than or equal to " + VIDEO_WATCH_THRESHOLD);
    }

    return null;
  }

  /**
   * Build a bad-request response for an invalid video engagement event and log a sanitised warning for observability.
   *
   * @param videoId the videoId the event referred to (or a placeholder if absent).
   * @param reason  a human-readable explanation of the validation failure.
   * @return a {@link SegueErrorResponse} with {@link Status#BAD_REQUEST}.
   */
  private SegueErrorResponse videoEventValidationError(final String videoId, final String reason) {
    log.warn("Rejecting {} event (videoId: {}): {}", VIDEO_ENGAGEMENT_EVENT_TYPE,
        sanitiseExternalLogValue(videoId), reason);
    return new SegueErrorResponse(Status.BAD_REQUEST,
        "Invalid " + VIDEO_ENGAGEMENT_EVENT_TYPE + " event: " + reason);
  }

  /**
   * Read a numeric field from a client supplied event payload. JSON numbers are deserialized as {@link Integer} or
   * {@link Double}, so this normalises any {@link Number} to a {@link Double}.
   *
   * @param eventJSON the client supplied event details.
   * @param fieldName the field to read.
   * @return the value as a Double, or null if the field is absent or not a number.
   */
  private static Double getNumericField(final Map<String, Object> eventJSON, final String fieldName) {
    Object value = eventJSON.get(fieldName);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return null;
  }
}
