/**
 * Copyright 2015 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.api;

import static java.time.ZoneOffset.UTC;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.ENDDATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_CANNOT_BOOK_CANCELLED_EVENT;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_CANNOT_LOCATE_USER;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_CONTENT_ERROR_RETRIEVING_BOOKING;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_DATABASE_ERROR_CREATING_BOOKING;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_DATABASE_ERROR_DELETING_BOOKING;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_DATABASE_ERROR_RETRIEVING_BOOKING;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.PRIVATE_EVENT_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ADMIN_BOOKING_REASON_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ATTENDED_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.BOOKING_STATUS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT_AS_STRING;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_START_INDEX_AS_STRING;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_ID_FKEY_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EventFilterOption;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.STAGE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueServerLogType;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SortOrder;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TITLE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.USER_ID_FKEY_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.USER_ID_LIST_FKEY_FIELDNAME;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.opencsv.CSVWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateBookingException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingUpdateException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventDeadlineException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventGroupReservationLimitException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventIsCancelledException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventIsFullException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventIsNotFullException;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.CompetitionEntryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.MainObjectMapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.SimpleExclusionInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Events Facade.
 */
@Path("/events")
@Tag(name = "/events")
public class EventsFacade extends AbstractIsaacFacade {
  private static final Logger log = LoggerFactory.getLogger(EventsFacade.class);

  private final EventBookingManager bookingManager;

  private final UserAccountManager userManager;

  private final GroupManager groupManager;

  private final GitContentManager contentManager;
  private final UserBadgeManager userBadgeManager;
  private final UserAssociationManager userAssociationManager;
  private final SchoolListReader schoolListReader;

  private final MainObjectMapper mapper;

  /**
   * EventsFacade.
   *
   * @param properties             global properties map
   * @param logManager             for managing logs.
   * @param bookingManager         Instance of Booking Manager
   * @param userManager            Instance of User Manager
   * @param contentManager         for retrieving event content
   * @param userBadgeManager       for updating badge information
   * @param userAssociationManager for checking permissions and filtering records
   * @param groupManager           Instance of Group Manager
   * @param schoolListReader       for retrieving school information
   * @param mapper                 Instance of Mapper Facade, to map between DO and DTO classes
   */
  @Inject
  public EventsFacade(final PropertiesLoader properties, final ILogManager logManager,
                      final EventBookingManager bookingManager, final UserAccountManager userManager,
                      final GitContentManager contentManager,
                      final UserBadgeManager userBadgeManager, final UserAssociationManager userAssociationManager,
                      final GroupManager groupManager,
                      final SchoolListReader schoolListReader, final MainObjectMapper mapper) {
    super(properties, logManager);
    this.bookingManager = bookingManager;
    this.userManager = userManager;
    this.contentManager = contentManager;
    this.userBadgeManager = userBadgeManager;
    this.userAssociationManager = userAssociationManager;
    this.groupManager = groupManager;
    this.schoolListReader = schoolListReader;
    this.mapper = mapper;
  }

  /**
   * REST end point to provide a list of events.
   *
   * @param request              this allows us to check to see if a user is currently loggedin.
   * @param tags                 a comma separated list of tags to include in the search.
   * @param startIndex           the initial index for the first result.
   * @param limit                the maximums number of results to return
   * @param sortOrder            flag to indicate preferred sort order.
   * @param showActiveOnly       true will impose filtering on the results. False will not. Defaults to false.
   * @param showInactiveOnly     true will impose filtering on the results. False will not. Defaults to false.
   * @param showMyBookingsOnly   true will impose filtering on the results. False will not. Defaults to false.
   * @param showReservationsOnly true will impose filtering on the results. False will not. Defaults to false.
   * @param showStageOnly        if present, only events with an audience matching this string will be shown
   * @return a Response containing a list of events objects or containing a SegueErrorResponse.
   */
  @SuppressWarnings("checkstyle:ParameterNumber")
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "List events matching the provided criteria.")
  public final Response getEvents(@Context final HttpServletRequest request,
                                  @QueryParam("tags") final String tags,
                                  @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index")
                                  final Integer startIndex,
                                  @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit")
                                  final Integer limit,
                                  @QueryParam("sort_by") final String sortOrder,
                                  @QueryParam("show_active_only") final Boolean showActiveOnly,
                                  @QueryParam("show_inactive_only") final Boolean showInactiveOnly,
                                  @QueryParam("show_booked_only") final Boolean showMyBookingsOnly,
                                  @QueryParam("show_reservations_only") final Boolean showReservationsOnly,
                                  @QueryParam("show_stage_only") final String showStageOnly) {
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();

    Integer newLimit = null;
    Integer newStartIndex = null;
    if (limit != null) {
      newLimit = limit;
    }

    if (startIndex != null) {
      newStartIndex = startIndex;
    }

    if (tags != null) {
      fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
    }

    if (showStageOnly != null) {
      fieldsToMatch.put(STAGE_FIELDNAME, Arrays.asList(showStageOnly.split(",")));
    }

    final Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    if (sortOrder != null && sortOrder.equals(TITLE_FIELDNAME)) {
      sortInstructions.put(TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
          SortOrder.ASC);
    } else {
      sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);
    }

    fieldsToMatch.put(TYPE_FIELDNAME, List.of(EVENT_TYPE));

    Map<String, AbstractFilterInstruction> filterInstructions = null;
    if (null != showActiveOnly && showActiveOnly) {
      filterInstructions = Maps.newHashMap();
      DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(Instant.now(), null);
      filterInstructions.put(ENDDATE_FIELDNAME, anyEventsFromNow);
      sortInstructions.put(DATE_FIELDNAME, SortOrder.ASC);
    }

    if (null != showInactiveOnly && showInactiveOnly) {
      if (null != showActiveOnly && showActiveOnly) {
        return new SegueErrorResponse(Status.BAD_REQUEST,
            "You cannot request both show active and inactive only.").toResponse();
      }

      filterInstructions = Maps.newHashMap();
      DateRangeFilterInstruction anyEventsToNow = new DateRangeFilterInstruction(null, Instant.now());
      filterInstructions.put(ENDDATE_FIELDNAME, anyEventsToNow);
      sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);
    }

    try {
      ResultsWrapper<ContentDTO> findByFieldNames = null;

      if (null != showMyBookingsOnly && showMyBookingsOnly) {
        RegisteredUserDTO currentUser = null;
        try {
          currentUser = this.userManager.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e) {
          /* Safe to ignore; will just leave currentUser null. */
        }
        if (null != currentUser) {
          findByFieldNames = getEventsBookedByUser(request, fieldsToMatch.get(TAGS_FIELDNAME), currentUser);
        } else {
          SegueErrorResponse.getNotLoggedInResponse();
        }
      } else if (null != showReservationsOnly && showReservationsOnly) {
        RegisteredUserDTO currentUser = null;
        try {
          currentUser = this.userManager.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e) {
          /* Safe to ignore; will just leave currentUser null. */
        }
        if (null != currentUser) {
          findByFieldNames = getEventsReservedByUser(request, currentUser);
        } else {
          SegueErrorResponse.getNotLoggedInResponse();
        }
      } else {
        if (filterInstructions == null) {
          filterInstructions = Maps.newHashMap();
        }
        // If neither bookings only nor reservations only, hide private events
        filterInstructions.put(PRIVATE_EVENT_FIELDNAME, new SimpleExclusionInstruction("true"));
        findByFieldNames = this.contentManager.findByFieldNames(
            ContentService.generateDefaultFieldToMatch(fieldsToMatch), newStartIndex, newLimit,
            sortInstructions, filterInstructions);

        // augment (maybe slow for large numbers of bookings)
        for (ContentDTO c : findByFieldNames.getResults()) {
          this.augmentEventWithBookingInformation(request, c);
        }
      }

      return Response.ok(findByFieldNames).build();
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT)
          .toResponse();
    } catch (SegueDatabaseException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error accessing your bookings.")
          .toResponse();
    }
  }

  /**
   * Get Events Booked by user.
   *
   * @param request     the http request so we can resolve booking information
   * @param tags        the tags we want to filter on
   * @param currentUser the currently logged on user.
   * @return a list of event pages that the user has been booked
   * @throws SegueDatabaseException if an error occurs while retrieving booking information
   * @throws ContentManagerException if an error occurs while retrieving event information
   */
  private ResultsWrapper<ContentDTO> getEventsBookedByUser(final HttpServletRequest request, final List<String> tags,
                                                           final RegisteredUserDTO currentUser)
      throws SegueDatabaseException, ContentManagerException {
    List<ContentDTO> filteredResults = Lists.newArrayList();

    Map<String, BookingStatus> userBookingMap = this.bookingManager.getAllEventStatesForUser(currentUser.getId());

    for (String eventId : userBookingMap.keySet()) {
      if (BookingStatus.CANCELLED.equals(userBookingMap.get(eventId))) {
        continue;
      }

      final IsaacEventPageDTO eventDTOById = this.getAugmentedEventDTOById(request, eventId);

      if (tags != null) {
        Set<String> tagsList = Sets.newHashSet(tags);
        tagsList.retainAll(eventDTOById.getTags()); // get intersection
        if (tagsList.isEmpty()) {
          // if the intersection is empty then we can continue
          continue;
        }
      }

      filteredResults.add(eventDTOById);
    }
    return new ResultsWrapper<>(filteredResults, (long) filteredResults.size());
  }

  /**
   * Get Events Reserved by user.
   *
   * @param request     the http request so we can resolve booking information
   * @param currentUser the currently logged on user.
   * @return a list of event pages that the user has been booked
   * @throws SegueDatabaseException if an error occurs while retrieving booking information
   * @throws ContentManagerException if an error occurs while retrieving event information
   */
  private ResultsWrapper<ContentDTO> getEventsReservedByUser(final HttpServletRequest request,
                                                             final RegisteredUserDTO currentUser)
      throws SegueDatabaseException, ContentManagerException {
    List<ContentDTO> filteredResults = Lists.newArrayList();

    List<EventBookingDTO> userReservationList =
        this.mapper.mapList(bookingManager.getAllEventReservationsForUser(currentUser.getId()),
            DetailedEventBookingDTO.class, EventBookingDTO.class);

    for (EventBookingDTO booking : userReservationList) {

      final IsaacEventPageDTO eventDTOById = this.getAugmentedEventDTOById(request, booking.getEventId());

      filteredResults.add(eventDTOById);
    }
    return new ResultsWrapper<>(filteredResults, (long) filteredResults.size());
  }

  /**
   * REST end point to retrieve an event by id..
   *
   * @param request this allows us to check to see if a user is currently logged-in.
   * @param eventId Id of the event of interest.
   * @return a Response containing a list of events objects or containing a SegueErrorResponse.
   */
  @GET
  @Path("/{event_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Get details about a specific event.")
  public final Response getEvent(@Context final HttpServletRequest request,
                                 @PathParam("event_id") final String eventId) {
    try {
      IsaacEventPageDTO page = getAugmentedEventDTOById(request, eventId);
      return Response.ok(page)
          .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
    } catch (ResourceNotFoundException e) {
      return new SegueErrorResponse(Status.NOT_FOUND, "The event specified does not exist.").toResponse();
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT)
          .toResponse();
    } catch (SegueDatabaseException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error resolving event bookings.")
          .toResponse();
    }
  }

  /**
   * Count all event bookings.
   *
   * @param request so we can determine if the user is logged in
   * @return a list of booking objects
   */
  @GET
  @Path("/bookings/count")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Count all event bookings.")
  public final Response getCountForAllEventBookings(@Context final HttpServletRequest request) {
    try {
      if (!isUserAnAdminOrEventManager(userManager, request)) {
        return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
            .toResponse();
      }

      return Response.ok(Map.of("count", bookingManager.getCountOfEventBookings())).build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = EXCEPTION_MESSAGE_DATABASE_ERROR_RETRIEVING_BOOKING;
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    }
  }

  /**
   * Find a booking by id.
   *
   * @param request   for authentication
   * @param bookingId the booking of interest.
   * @return The booking information.
   */
  @GET
  @Path("/bookings/{booking_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Get details about an event booking.")
  public final Response getEventBookingsById(@Context final HttpServletRequest request,
                                             @PathParam("booking_id") final String bookingId) {
    try {
      if (!isUserAnAdminOrEventManager(userManager, request)) {
        return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
            .toResponse();
      }

      return Response.ok(bookingManager.getDetailedBookingById(Long.parseLong(bookingId))).build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (NumberFormatException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "The booking id provided is invalid.").toResponse();
    } catch (ResourceNotFoundException e) {
      return new SegueErrorResponse(Status.NOT_FOUND, "The booking you requested does not exist.").toResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = EXCEPTION_MESSAGE_DATABASE_ERROR_RETRIEVING_BOOKING;
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    }
  }

  /**
   * Allow a staff user to promote a existing bookings to confirmed bookings.
   *
   * @param request               so we can determine if the user is logged in
   * @param eventId               event booking containing updates, must contain primary id.
   * @param userId                the user to be promoted.
   * @return the updated booking.
   */
  @POST
  @Path("{event_id}/bookings/{user_id}/promote")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Move a user from an event waiting list, reservation or cancellation to a confirmed booking.")
  public final Response promoteBooking(@Context final HttpServletRequest request,
                                       @PathParam("event_id") final String eventId,
                                       @PathParam("user_id") final Long userId) {
    try {
      RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);
      RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(userId);
      IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

      if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      EventBookingDTO eventBookingDTO
          = this.bookingManager.promoteToConfirmedBooking(event, userOfInterest);

      this.getLogManager().logEvent(currentUser, request,
          SegueServerLogType.ADMIN_EVENT_WAITING_LIST_PROMOTION, Map.of(EVENT_ID_FKEY_FIELDNAME, event.getId(),
              USER_ID_FKEY_FIELDNAME, userId));
      return Response.ok(eventBookingDTO).build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = "Database error occurred while trying to update a event booking";
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          EXCEPTION_MESSAGE_CONTENT_ERROR_RETRIEVING_BOOKING)
          .toResponse();
    } catch (EventIsFullException e) {
      return new SegueErrorResponse(Status.CONFLICT,
          "This event is already full. Unable to book the user on to it.")
          .toResponse();
    } catch (EventBookingUpdateException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "Unable to modify the booking", e)
          .toResponse();
    } catch (NoUserException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "The user doesn't exist, so unable to book them onto an event", e)
          .toResponse();
    } catch (EventIsCancelledException e) {
      return SegueErrorResponse.getBadRequestResponse(EXCEPTION_MESSAGE_CANNOT_BOOK_CANCELLED_EVENT);
    }
  }

  /**
   * gets an admin and selected staff only list of event bookings based on a given event id.
   *
   * @param request so we can determine if the user is logged in
   * @param eventId string id of the event to get bookings for
   * @return list of bookings.
   */
  @GET
  @Path("{event_id}/bookings")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "List event bookings for a specific event.")
  public final Response adminGetEventBookingByEventId(@Context final HttpServletRequest request,
                                                      @PathParam("event_id") final String eventId) {
    try {
      RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
      IsaacEventPageDTO event = getRawEventDTOById(eventId);

      if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      List<DetailedEventBookingDTO> eventBookings = bookingManager.adminGetBookingsByEventId(eventId);

      if (Role.EVENT_LEADER.equals(currentUser.getRole())) {
        eventBookings = userAssociationManager.filterUnassociatedRecords(currentUser, eventBookings,
            booking -> booking.getUserBooked().getId());
      }

      return Response.ok(eventBookings).build();

    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException | ContentManagerException e) {
      String message = EXCEPTION_MESSAGE_DATABASE_ERROR_RETRIEVING_BOOKING;
      log.error(message, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
    }
  }

  /**
   * gets a list of event bookings based on a given group id.
   *
   * @param request servlet request object for retrieving current user
   * @param eventId string id for event to get bookings for
   * @param groupId string id for group to retrieve for checking permissions and filtering results
   * @return a list of EventBookingDTOs if successful or a SegueErrorResponse if not
   */
  @GET
  @Path("{event_id}/bookings/for_group/{group_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "List event bookings for a specific event and group.")
  public final Response getEventBookingForGivenGroup(@Context final HttpServletRequest request,
                                                     @PathParam("event_id") final String eventId,
                                                     @PathParam("group_id") final String groupId) {
    try {
      RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
      UserGroupDTO group = groupManager.getGroupById(Long.parseLong(groupId));

      if (!(isUserAnAdmin(userManager, currentUser) || GroupManager.isOwnerOrAdditionalManager(group,
          currentUser.getId()))) {
        return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner or manager of this group.").toResponse();
      }

      IsaacEventPageDTO eventPageDTO = getRawEventDTOById(eventId);
      if (null == eventPageDTO) {
        return new SegueErrorResponse(Status.BAD_REQUEST, "No event found with this ID.").toResponse();
      }
      if (!EventBookingManager.eventAllowsGroupBookings(eventPageDTO)) {
        return new SegueErrorResponse(Status.FORBIDDEN, "This event does not accept group bookings.").toResponse();
      }

      List<Long> groupMemberIds = groupManager.getUsersInGroup(group)
          .stream().map(RegisteredUserDTO::getId)
          .collect(Collectors.toList());

      // Filter eventBookings based on whether the booked user is a member of the given group
      List<EventBookingDTO> eventBookings = bookingManager.getBookingsByEventId(eventId)
          .stream().filter(booking -> groupMemberIds.contains(booking.getUserBooked().getId()))
          .collect(Collectors.toList());

      // Event leaders are only allowed to see the bookings of connected users
      eventBookings = userAssociationManager.filterUnassociatedRecords(currentUser, eventBookings,
          booking -> booking.getUserBooked().getId());

      return Response.ok(this.mapper.mapList(eventBookings, EventBookingDTO.class, EventBookingDTO.class)).build();
    } catch (SegueDatabaseException e) {
      String errorMsg = String.format(
          "Database error occurred while trying retrieve bookings for group (%s) on event (%s).",
          sanitiseExternalLogValue(groupId), sanitiseExternalLogValue(eventId));
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Content Database error occurred while trying to retrieve event information.")
          .toResponse();
    }
  }

  /**
   * gets a list of event bookings for all groups owned.
   *
   * @param request servlet request object for retrieving current user
   * @param eventId string id for event to get bookings for
   * @return a list of EventBookingDTOs if successful or a SegueErrorResponse if not
   */
  @GET
  @Path("{event_id}/groups_bookings")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "List event bookings for a specific event")
  public final Response getEventBookingForAllGroups(@Context final HttpServletRequest request,
                                                    @PathParam("event_id") final String eventId) {
    try {
      RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);

      // Tutors cannot yet manage event bookings for their groups
      if (!isUserTeacherOrAbove(userManager, currentUser)) {
        return new SegueErrorResponse(Status.FORBIDDEN,
            "You do not have permission to use this endpoint.").toResponse();
      }

      List<EventBookingDTO> eventBookings =
          this.mapper.mapList(bookingManager.getBookingsByEventId(eventId), DetailedEventBookingDTO.class,
              EventBookingDTO.class);

      // Only allowed to see the bookings of connected users
      eventBookings = userAssociationManager.filterUnassociatedRecords(
          currentUser, eventBookings, booking -> booking.getUserBooked().getId());

      // Convert extended summary objects to their basic form
      eventBookings.forEach(
          booking -> booking.setUserBooked(mapper.map(booking.getUserBooked(), UserSummaryDTO.class)));

      return Response.ok(eventBookings).build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = String.format(
          "Database error occurred while trying retrieve bookings for event (%s).",
          sanitiseExternalLogValue(eventId));
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    }
  }

  /**
   * Allows authorised users to view a csv of event attendees.
   *
   * @param request so we can determine if the user is logged in
   * @param eventId the event of interest.
   * @return list of bookings csv.
   */
  @GET
  @Path("{event_id}/bookings/download")
  @Produces("text/csv")
  @GZIP
  @Operation(summary = "Download event attendance csv.")
  public Response getEventBookingCSV(@Context final HttpServletRequest request,
                                     @PathParam("event_id") final String eventId) {
    try {
      RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
      IsaacEventPageDTO event = this.getRawEventDTOById(eventId);

      if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      List<DetailedEventBookingDTO> eventBookings = bookingManager.adminGetBookingsByEventId(eventId);

      // Event leaders are only allowed to see the bookings of connected users
      if (Role.EVENT_LEADER.equals(currentUser.getRole())) {
        eventBookings = userAssociationManager.filterUnassociatedRecords(
            currentUser, eventBookings, booking -> booking.getUserBooked().getId());
      }

      List<String[]> rows = Lists.newArrayList();
      StringWriter stringWriter = new StringWriter();
      CSVWriter csvWriter = new CSVWriter(stringWriter);
      StringBuilder headerBuilder = new StringBuilder();
      headerBuilder.append(String.format("Event (%s) Attendance: Downloaded on %s \nGenerated by: %s %s \n\n",
          eventId, Instant.now(), currentUser.getGivenName(),
          currentUser.getFamilyName()));

      List<String> headerRow = Lists.newArrayList(Arrays.asList("", ""));

      rows.add(headerRow.toArray(new String[0]));

      List<String> totalsRow = Lists.newArrayList();

      List<String[]> resultRows = Lists.newArrayList();
      DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(UTC);

      for (DetailedEventBookingDTO booking : eventBookings) {
        ArrayList<String> resultRow = Lists.newArrayList();
        UserSummaryDTO resultUser = booking.getUserBooked();
        RegisteredUserDTO resultRegisteredUser;
        try {
          resultRegisteredUser = this.userManager.getUserDTOById(resultUser.getId());
        } catch (NoUserException e) {
          // Possibly a deleted user, silently skipping.
          continue;
        }
        String schoolId = resultRegisteredUser.getSchoolId();
        Map<String, String> resultAdditionalInformation = booking.getAdditionalInformation();
        BookingStatus resultBookingStatus = booking.getBookingStatus();
        resultRow.add(resultUser.getFamilyName());
        resultRow.add(resultUser.getGivenName());
        resultRow.add(resultRegisteredUser.getRole().toString());
        if (schoolId != null) {
          School school = schoolListReader.findSchoolById(schoolId);
          if (null != school) {
            resultRow.add(school.getName());
          } else {
            resultRow.add(schoolId);
          }
        } else {
          resultRow.add(resultRegisteredUser.getSchoolOther());
        }
        resultRow.add(resultBookingStatus.toString());
        resultRow.add(dateFormat.format(booking.getBookingDate()));
        resultRow.add(dateFormat.format(booking.getUpdated()));
        resultRow.add(resultAdditionalInformation.get("yearGroup"));
        resultRow.add(resultAdditionalInformation.get("jobTitle"));
        resultRow.add(String.join(" ", resultUser.getRegisteredContexts().stream()
            .map(uc -> uc.getStage() != null ? uc.getStage().name() : "").collect(Collectors.toSet())));
        resultRow.add(String.join(" ", resultUser.getRegisteredContexts().stream()
            .map(uc -> uc.getExamBoard() != null ? uc.getExamBoard().name() : "").collect(Collectors.toSet())));
        resultRow.add(resultAdditionalInformation.get("dietaryRequirements"));
        resultRow.add(resultAdditionalInformation.get("accessibilityRequirements"));
        resultRow.add(resultAdditionalInformation.get("emergencyName"));
        resultRow.add(resultAdditionalInformation.get("emergencyNumber"));
        Collections.addAll(resultRows, resultRow.toArray(new String[0]));
      }

      rows.add(totalsRow.toArray(new String[0]));
      rows.add(("Family name,Given name,Role,School,Booking status,Booking date,Last updated date,Year group,"
          // lgtm [java/missing-space-in-concatenation]
          + "Job title,Stages,Exam boards,dietary requirements,"
          + "Accessibility requirements,Emergency name,Emergency number").split(","));
      rows.addAll(resultRows);
      csvWriter.writeAll(rows);
      csvWriter.close();

      headerBuilder.append(stringWriter.toString());
      return Response.ok(headerBuilder.toString())
          .header("Content-Disposition", String.format("attachment; filename=event_attendees_%s.csv", eventId))
          .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();

    } catch (IOException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while building the CSV file.").toResponse();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String message = EXCEPTION_MESSAGE_DATABASE_ERROR_RETRIEVING_BOOKING;
      log.error(message, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          EXCEPTION_MESSAGE_CONTENT_ERROR_RETRIEVING_BOOKING)
          .toResponse();
    } catch (UnableToIndexSchoolsException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error while looking up schools", e)
          .toResponse();
    }
  }

  /**
   * createBooking for a specific isaac user.
   * - Will attempt to create a waiting list booking if the event is already full.
   * - Must be a Staff user.
   *
   * @param request               so we can determine if the user is logged in
   * @param eventId               event id
   * @param userId                user id
   * @param additionalInformation additional information to be stored with this booking e.g. dietary requirements.
   * @return the new booking
   */
  @POST
  @Path("{event_id}/bookings/{user_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Create an event booking for a user.")
  public final Response createBookingForGivenUser(@Context final HttpServletRequest request,
                                                  @PathParam("event_id") final String eventId,
                                                  @PathParam("user_id") final Long userId,
                                                  final Map<String, String> additionalInformation) {
    try {
      RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
      RegisteredUserDTO bookedUser = userManager.getUserDTOById(userId);
      IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

      if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      if (bookingManager.isUserBooked(eventId, userId)) {
        return new SegueErrorResponse(Status.BAD_REQUEST, "User is already booked on this event.")
            .toResponse();
      }

      EventBookingDTO booking =
          bookingManager.createBookingOrAddToWaitingList(event, bookedUser, additionalInformation);
      this.getLogManager().logEvent(currentUser, request,
          SegueServerLogType.ADMIN_EVENT_BOOKING_CREATED,
          Map.of(
              EVENT_ID_FKEY_FIELDNAME, event.getId(),
              USER_ID_FKEY_FIELDNAME, userId,
              BOOKING_STATUS_FIELDNAME, booking.getBookingStatus().toString(),
              ADMIN_BOOKING_REASON_FIELDNAME, additionalInformation.get("authorisation") == null ? "NOT_PROVIDED" :
                  additionalInformation.get("authorisation")
          ));

      return Response.ok(this.mapper.copy(booking)).build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = EXCEPTION_MESSAGE_DATABASE_ERROR_CREATING_BOOKING;
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (NoUserException e) {
      return new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the user requested").toResponse();
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          EXCEPTION_MESSAGE_CONTENT_ERROR_RETRIEVING_BOOKING)
          .toResponse();
    } catch (DuplicateBookingException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "User already booked on this event. Unable to create a duplicate booking.")
          .toResponse();
    } catch (EventIsCancelledException e) {
      return SegueErrorResponse.getBadRequestResponse(EXCEPTION_MESSAGE_CANNOT_BOOK_CANCELLED_EVENT);
    }
  }

  /**
   * Add event reservations for the given users.
   *
   * @param request so we can determine who is making the request
   * @param eventId event id
   * @param userIds the users to reserve spaces for
   * @return the list of bookings/reservations
   */
  @POST
  @Path("{event_id}/reservations")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Add event reservations for the given users.")
  public final Response createReservationsForGivenUsers(@Context final HttpServletRequest request,
                                                        @PathParam("event_id") final String eventId,
                                                        final List<Long> userIds) {
    RegisteredUserDTO reservingUser;
    IsaacEventPageDTO event;
    try {
      event = this.getRawEventDTOById(eventId);
    } catch (SegueDatabaseException | ContentManagerException e) {
      event = null;
    }
    if (null == event) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "No event found with this ID.").toResponse();
    }
    if (!EventBookingManager.eventAllowsGroupBookings(event)) {
      return new SegueErrorResponse(Status.FORBIDDEN, "This event does not accept group bookings.").toResponse();
    }

    List<RegisteredUserDTO> usersToReserve = Lists.newArrayList();
    try {
      reservingUser = userManager.getCurrentRegisteredUser(request);
      // Tutors cannot yet manage event bookings for their tutees, so shouldn't be added to this list
      if (!Arrays.asList(Role.TEACHER, Role.EVENT_LEADER, Role.EVENT_MANAGER, Role.ADMIN)
          .contains(reservingUser.getRole())) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      // Enforce permission
      for (Long userId : userIds) {
        RegisteredUserDTO userToReserve = userManager.getUserDTOById(userId);
        if (userAssociationManager.hasPermission(reservingUser, userToReserve)) {
          usersToReserve.add(userToReserve);
        } else {
          return new SegueErrorResponse(Status.FORBIDDEN,
              "You do not have permission to book or reserve some of these users onto this event.")
              .toResponse();
        }
      }

      List<EventBookingDTO> bookings = bookingManager.requestReservations(event, usersToReserve, reservingUser);

      this.getLogManager().logEvent(reservingUser, request,
          SegueServerLogType.EVENT_RESERVATIONS_CREATED,
          Map.of(
              EVENT_ID_FKEY_FIELDNAME, event.getId(),
              USER_ID_FKEY_FIELDNAME, reservingUser.getId(),
              USER_ID_LIST_FKEY_FIELDNAME, userIds.toArray(),
              BOOKING_STATUS_FIELDNAME, BookingStatus.RESERVED.toString()
          ));
      return Response.ok(this.mapper.mapList(bookings, EventBookingDTO.class, EventBookingDTO.class)).build();

    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = "Database error occurred while trying to reserve space for a user onto an event.";
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (EventIsFullException e) {
      return new SegueErrorResponse(Status.CONFLICT,
          "There are not enough spaces available for this event. Please try again with fewer users.")
          .toResponse();
    } catch (EventGroupReservationLimitException e) {
      return new SegueErrorResponse(Status.CONFLICT,
          String.format("You can only request a maximum of %d student reservations for this event.",
              event.getGroupReservationLimit())).toResponse();
    } catch (EventDeadlineException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "The booking deadline for this event has passed. No more bookings or reservations are being accepted.")
          .toResponse();
    } catch (DuplicateBookingException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "One of the users requested is already booked or reserved on this event."
              + " Unable to create a duplicate booking.")
          .toResponse();
    } catch (NoUserException e) {
      return SegueErrorResponse.getResourceNotFoundResponse("Unable to locate one of the users specified.");
    } catch (EventIsCancelledException e) {
      return SegueErrorResponse.getBadRequestResponse(EXCEPTION_MESSAGE_CANNOT_BOOK_CANCELLED_EVENT);
    }
  }

  /**
   * Add competition reservations for the given users.
   *
   * @param request so we can determine who is making the request
   * @param eventId event ID
   * @param entryDTO DTO containing candidate IDs and submission link
   * @return the list of bookings/reservations
   */
  @POST
  @Path("{event_id}/competitionEntries")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Add event reservations in the competition for the given users.")
  public final Response createCompetitionEntry(@Context final HttpServletRequest request,
                                               @PathParam("event_id") final String eventId,
                                               final CompetitionEntryDTO entryDTO) {
    RegisteredUserDTO reservingUser;
    IsaacEventPageDTO event;
    Map<String, String> additionalInformation = new HashMap<>();


    try {
      event = this.getRawEventDTOById(eventId);
    } catch (SegueDatabaseException | ContentManagerException e) {
      event = null;
    }
    if (null == event) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "No event found with this ID.").toResponse();
    }
    if (!EventBookingManager.eventAllowsGroupBookings(event)) {
      return new SegueErrorResponse(Status.FORBIDDEN, "This event does not accept group bookings.").toResponse();
    }

    List<RegisteredUserDTO> usersToReserve = Lists.newArrayList();
    try {
      reservingUser = userManager.getCurrentRegisteredUser(request);
      additionalInformation.put("submissionURL", entryDTO.getSubmissionURL());
      additionalInformation.put("groupName", entryDTO.getGroupName());
      additionalInformation.put("teacherName", reservingUser.getGivenName() + " " + reservingUser.getFamilyName());
      additionalInformation.put("teacherEmail", reservingUser.getEmail());
      additionalInformation.put("teacherId", reservingUser.getId().toString());
      additionalInformation.put("school", reservingUser.getSchoolId());
      additionalInformation.put("schoolName", reservingUser.getSchoolOther());

      // Tutors cannot yet manage event bookings for their tutees, so shouldn't be added to this list
      if (!Arrays.asList(Role.TEACHER, Role.EVENT_LEADER, Role.EVENT_MANAGER, Role.ADMIN)
          .contains(reservingUser.getRole())) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      List<EventBookingDTO> bookings = new ArrayList<>();
      // Enforce permission
      for (Long userId : entryDTO.getEntrantIds()) {
        RegisteredUserDTO userToReserve = userManager.getUserDTOById(userId);
        if (userAssociationManager.hasPermission(reservingUser, userToReserve)) {
          usersToReserve.add(userToReserve);
          BookingStatus status = bookingManager.getBookingStatus(event.getId(), userToReserve.getId());
          if (null != status) {
            bookingManager.deleteBooking(event, userToReserve);
          }
          bookings.add(
              bookingManager.createBooking(event, userToReserve, additionalInformation, BookingStatus.CONFIRMED)
          );
        } else {
          return new SegueErrorResponse(Status.FORBIDDEN,
              "You do not have permission to book or reserve some of these users onto this event.")
              .toResponse();
        }
      }

      this.getLogManager().logEvent(reservingUser, request,
          SegueServerLogType.EVENT_RESERVATIONS_CREATED,
          Map.of(
              EVENT_ID_FKEY_FIELDNAME, event.getId(),
              USER_ID_FKEY_FIELDNAME, reservingUser.getId(),
              USER_ID_LIST_FKEY_FIELDNAME, entryDTO.getEntrantIds().toArray(),
              BOOKING_STATUS_FIELDNAME, BookingStatus.RESERVED.toString()
          ));

      return Response.ok(this.mapper.mapList(bookings, EventBookingDTO.class, EventBookingDTO.class)).build();

    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = "Database error occurred while trying to reserve space for a user onto an event.";
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (EventIsFullException e) {
      return new SegueErrorResponse(Status.CONFLICT,
          "There are not enough spaces available for this event. Please try again with fewer users.")
          .toResponse();
    } catch (DuplicateBookingException e) {
      return SegueErrorResponse.getBadRequestResponse(
                "One of the users requested is already booked or reserved on this event."
                  + " Unable to create a duplicate booking.");
    } catch (NoUserException e) {
      return SegueErrorResponse.getResourceNotFoundResponse("Unable to locate one of the users specified.");
    } catch (EventIsCancelledException e) {
      return SegueErrorResponse.getBadRequestResponse(EXCEPTION_MESSAGE_CANNOT_BOOK_CANCELLED_EVENT);
    }
  }

  /**
   * This function allows cancellation of the reservations for the given users.
   *
   * @param request so we can determine if the user is logged in
   * @param eventId event id
   * @param userIds user ids
   * @return a 'No content' response if successful or a SegueErrorResponse if not
   */
  @POST
  @Path("{event_id}/reservations/cancel")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Cancel a reservations on an event for a set of users.")
  public final Response cancelReservations(@Context final HttpServletRequest request,
                                           @PathParam("event_id") final String eventId,
                                           final List<Long> userIds) {
    try {
      IsaacEventPageDTO event = getRawEventDTOById(eventId);
      RegisteredUserDTO userLoggedIn = this.userManager.getCurrentRegisteredUser(request);

      if (event.getDate() != null && Instant.now().isAfter(event.getDate())) {
        return new SegueErrorResponse(Status.BAD_REQUEST,
            "You cannot cancel a reservation on an event that has already started.")
            .toResponse();
      }

      boolean userIsAbleToManageEvent = bookingManager.isUserAbleToManageEvent(userLoggedIn, event);

      List<RegisteredUserDTO> validUsers = new ArrayList<>();
      for (Long userId : userIds) {
        RegisteredUserDTO userOwningBooking = userManager.getUserDTOById(userId);
        if (userIsAbleToManageEvent
            || bookingManager.isReservationMadeByRequestingUser(userLoggedIn, userOwningBooking, event)
            && userAssociationManager.hasPermission(userLoggedIn, userOwningBooking)) {
          if (bookingManager.hasBookingWithAnyOfStatuses(eventId, userId, new HashSet<>(Arrays.asList(
              BookingStatus.CONFIRMED, BookingStatus.WAITING_LIST, BookingStatus.RESERVED)))) {
            validUsers.add(userOwningBooking);
          } else {
            // Maybe silently carry on instead?
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "Some of the reservations cannot be cancelled. Please reload the page and try again.")
                .toResponse();
          }
        } else {
          return new SegueErrorResponse(Status.FORBIDDEN,
              "You are not authorized to cancel some of the reservations specified.")
              .toResponse();
        }
      }

      for (RegisteredUserDTO user : validUsers) {
        bookingManager.cancelBooking(event, user);
      }

      this.getLogManager().logEvent(userLoggedIn, request,
          SegueServerLogType.EVENT_RESERVATIONS_CANCELLED,
          Map.of(
              EVENT_ID_FKEY_FIELDNAME, event.getId(),
              USER_ID_FKEY_FIELDNAME, userLoggedIn.getId(),
              USER_ID_LIST_FKEY_FIELDNAME, validUsers.stream().map(RegisteredUserDTO::getId).toArray(),
              BOOKING_STATUS_FIELDNAME, BookingStatus.CANCELLED.toString()
          ));
      return Response.noContent().build();

    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT)
          .toResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = EXCEPTION_MESSAGE_DATABASE_ERROR_DELETING_BOOKING;
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (NoUserException e) {
      return SegueErrorResponse.getResourceNotFoundResponse(EXCEPTION_MESSAGE_CANNOT_LOCATE_USER);
    }
  }

  /**
   * createBooking for the current user.
   *
   * @param request               so we can determine if the user is logged in
   * @param eventId               event id
   * @param additionalInformation a Map of additional information for use when creating the booking
   * @return the new booking if allowed to book.
   */
  @POST
  @Path("{event_id}/bookings")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Create an event booking for the current user.")
  public final Response createBookingForMe(@Context final HttpServletRequest request,
                                           @PathParam("event_id") final String eventId,
                                           final Map<String, String> additionalInformation) {
    try {
      RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
      IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

      if (EventStatus.CLOSED.equals(event.getEventStatus())) {
        return new SegueErrorResponse(Status.BAD_REQUEST,
            "Sorry booking for this event is closed. Please try again later.")
            .toResponse();
      }

      if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
        return new SegueErrorResponse(Status.BAD_REQUEST, "Sorry, this event is cancelled.")
            .toResponse();
      }

      if (EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus())) {
        return new SegueErrorResponse(Status.BAD_REQUEST,
            "Sorry booking for this event is restricted. You can only be added to a waiting list.")
            .toResponse();
      }

      if (bookingManager.isUserBooked(eventId, user.getId())) {
        return new SegueErrorResponse(Status.BAD_REQUEST, "You are already booked on this event.")
            .toResponse();
      }

      // reservedBy is null. If there is a reservation for me, it will be updated to CONFIRMED.
      EventBookingDTO eventBookingDTO = bookingManager.requestBooking(event, user, additionalInformation);

      this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
          SegueServerLogType.EVENT_BOOKING, Map.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));

      return Response.ok(this.mapper.copy(eventBookingDTO)).build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = EXCEPTION_MESSAGE_DATABASE_ERROR_CREATING_BOOKING;
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          EXCEPTION_MESSAGE_CONTENT_ERROR_RETRIEVING_BOOKING)
          .toResponse();
    } catch (EmailMustBeVerifiedException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "In order to book on this event your user account must have a verified email address."
              + " Please verify your address to make a booking.")
          .toResponse();
    } catch (DuplicateBookingException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "You have already been booked on this event. Unable to create a duplicate booking.")
          .toResponse();
    } catch (EventIsFullException e) {
      return new SegueErrorResponse(Status.CONFLICT,
          "This event is already full. Unable to book you on to it.")
          .toResponse();
    } catch (EventDeadlineException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "The booking deadline for this event has passed. No more bookings are being accepted.")
          .toResponse();
    } catch (EventIsCancelledException e) {
      return SegueErrorResponse.getBadRequestResponse(EXCEPTION_MESSAGE_CANNOT_BOOK_CANCELLED_EVENT);
    }
  }

  /**
   * Add current user to waiting list for the given event.
   *
   * @param request               so we can determine if the user is logged in
   * @param eventId               event id
   * @param additionalInformation a Map of additional information for use when creating the booking
   * @return the new booking
   */
  @POST
  @Path("{event_id}/waiting_list")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Add the current user to an event waiting list.")
  public final Response addMeToWaitingList(@Context final HttpServletRequest request,
                                           @PathParam("event_id") final String eventId,
                                           final Map<String, String> additionalInformation) {
    try {
      RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

      IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

      EventBookingDTO eventBookingDTO = bookingManager.requestWaitingListBooking(event, user, additionalInformation);
      this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
          SegueServerLogType.EVENT_WAITING_LIST_BOOKING, Map.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));

      return Response.ok(this.mapper.copy(eventBookingDTO)).build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = EXCEPTION_MESSAGE_DATABASE_ERROR_CREATING_BOOKING;
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          EXCEPTION_MESSAGE_CONTENT_ERROR_RETRIEVING_BOOKING)
          .toResponse();
    } catch (EmailMustBeVerifiedException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "In order to book on this event your user account must have a verified email address."
              + " Please verify your address to make a booking.")
          .toResponse();
    } catch (DuplicateBookingException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "You have already been booked on this event. Unable to create a duplicate booking.")
          .toResponse();
    } catch (EventDeadlineException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "The booking deadline for this event has passed. No more bookings are being accepted.")
          .toResponse();
    } catch (EventIsNotFullException e) {
      return new SegueErrorResponse(Status.CONFLICT,
          "There are spaces on this event and the deadline has not passed."
              + " Please use the request booking endpoint to book you on to it.")
          .toResponse();
    } catch (EventIsCancelledException e) {
      return SegueErrorResponse.getBadRequestResponse(EXCEPTION_MESSAGE_CANNOT_BOOK_CANCELLED_EVENT);
    }
  }

  /**
   * This function allows a user who has booked onto an event to cancel their booking.
   *
   * @param request so we can determine if the user is logged in
   * @param eventId event id
   * @return the new booking
   */
  @DELETE
  @Path("{event_id}/bookings/cancel")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Cancel the current user's booking on an event.")
  public final Response cancelBooking(@Context final HttpServletRequest request,
                                      @PathParam("event_id") final String eventId) {
    return this.cancelBooking(request, eventId, null);
  }

  /**
   * This function allows cancellation of a booking.
   *
   * @param request so we can determine if the user is logged in
   * @param eventId event id
   * @param userId  user id
   * @return the new booking
   */
  @DELETE
  @Path("{event_id}/bookings/{user_id}/cancel")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Cancel a user's booking on an event.")
  public final Response cancelBooking(@Context final HttpServletRequest request,
                                      @PathParam("event_id") final String eventId,
                                      @PathParam("user_id") final Long userId) {
    try {
      IsaacEventPageDTO event = getRawEventDTOById(eventId);

      RegisteredUserDTO userLoggedIn = this.userManager.getCurrentRegisteredUser(request);
      RegisteredUserDTO userOwningBooking;

      if (null == userId) {
        userOwningBooking = userLoggedIn;
      } else {
        userOwningBooking = this.userManager.getUserDTOById(userId);
      }

      if (event.getDate() != null && Instant.now().isAfter(event.getDate())) {
        return new SegueErrorResponse(Status.BAD_REQUEST,
            "You cannot cancel a booking on an event that has already started.")
            .toResponse();
      }

      // if the user id is null then it means they are changing their own booking.
      if (userId != null && (!(bookingManager.isUserAbleToManageEvent(userLoggedIn, event)
          || bookingManager.isReservationMadeByRequestingUser(userLoggedIn, userOwningBooking, event)))) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      Set<BookingStatus> cancelableStatuses =
          new HashSet<>(Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.WAITING_LIST, BookingStatus.RESERVED));
      if (!bookingManager.hasBookingWithAnyOfStatuses(eventId, userOwningBooking.getId(), cancelableStatuses)) {
        return new SegueErrorResponse(Status.BAD_REQUEST, "User is not booked on this event.").toResponse();
      }

      bookingManager.cancelBooking(event, userOwningBooking);

      if (!userOwningBooking.equals(userLoggedIn)) {
        this.getLogManager().logEvent(userLoggedIn, request,
            SegueServerLogType.ADMIN_EVENT_BOOKING_CANCELLED, Map.of(
                EVENT_ID_FKEY_FIELDNAME, event.getId(), USER_ID_FKEY_FIELDNAME, userOwningBooking.getId()));
      } else {
        this.getLogManager().logEvent(userLoggedIn, request,
            SegueServerLogType.EVENT_BOOKING_CANCELLED, Map.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));
      }

      return Response.noContent().build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = EXCEPTION_MESSAGE_DATABASE_ERROR_DELETING_BOOKING;
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT)
          .toResponse();
    } catch (NoUserException e) {
      return SegueErrorResponse.getResourceNotFoundResponse(EXCEPTION_MESSAGE_CANNOT_LOCATE_USER);
    }
  }

  /**
   * This function allows an administrator to attempt to resend the last confirmation email send for a given booking.
   *
   * @param request so we can determine if the user is logged in
   * @param eventId event id
   * @param userId  user id
   * @return the new booking
   */
  @POST
  @Path("{event_id}/bookings/{user_id}/resend_confirmation")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Resend an event booking confirmation to a user.")
  public final Response resendEventEmail(@Context final HttpServletRequest request,
                                         @PathParam("event_id") final String eventId,
                                         @PathParam("user_id") final Long userId) {
    try {
      IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);
      RegisteredUserDTO bookedUser = this.userManager.getUserDTOById(userId);
      RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);

      if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      this.bookingManager.resendEventEmail(event, bookedUser);

      log.info("User ({}) has just resent an event email to user id ({})", currentUser.getEmail(), bookedUser.getId());

      return Response.noContent().build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = "Database error occurred while trying to resend an event email.";
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT)
          .toResponse();
    } catch (NoUserException e) {
      return SegueErrorResponse.getResourceNotFoundResponse(EXCEPTION_MESSAGE_CANNOT_LOCATE_USER);
    } catch (EventIsCancelledException e) {
      return SegueErrorResponse.getBadRequestResponse("Event is cancelled, cannot resent event emails.");
    }
  }

  /**
   * Delete a booking.
   * <br>
   * This is an admin function to allow staff to delete a booking permanently.
   *
   * @param request so we can determine if the user is logged in
   * @param eventId event id
   * @param userId  user id
   * @return the new booking
   */
  @DELETE
  @Path("{event_id}/bookings/{user_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Erase a user's booking on an event.",
      description = "This method removes the booking entirely, rather than recording the booking as cancelled.")
  public final Response deleteBooking(@Context final HttpServletRequest request,
                                      @PathParam("event_id") final String eventId,
                                      @PathParam("user_id") final Long userId) {
    try {
      RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
      if (!isUserAnAdmin(userManager, currentUser)) {
        return new SegueErrorResponse(Status.FORBIDDEN, "You must be an Admin user to access this endpoint.")
            .toResponse();
      }

      Set<BookingStatus> allValidBookingStatuses = new HashSet<>(Arrays.asList(BookingStatus.values()));
      if (!bookingManager.hasBookingWithAnyOfStatuses(eventId, userId, allValidBookingStatuses)) {
        return new SegueErrorResponse(Status.BAD_REQUEST, "User is not booked on this event.").toResponse();
      }

      IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);
      RegisteredUserDTO user = this.userManager.getUserDTOById(userId);

      bookingManager.deleteBooking(event, user);

      this.getLogManager().logEvent(currentUser, request, SegueServerLogType.ADMIN_EVENT_BOOKING_DELETED,
          Map.of(EVENT_ID_FKEY_FIELDNAME, eventId, USER_ID_FKEY_FIELDNAME, userId));

      return Response.noContent().build();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = EXCEPTION_MESSAGE_DATABASE_ERROR_DELETING_BOOKING;
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (NoUserException e) {
      return SegueErrorResponse.getResourceNotFoundResponse(EXCEPTION_MESSAGE_CANNOT_LOCATE_USER);
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT)
          .toResponse();
    }
  }

  /**
   * Allow a staff user to record event attendance.
   *
   * @param request  so we can determine if the user is logged in
   * @param eventId  event booking containing updates, must contain primary id.
   * @param userIds   the user IDs to be promoted.
   * @param attended boolean value representing whether the user was present, true, or absent, false.
   * @return the updated booking.
   */
  @POST
  @Path("{event_id}/bookings/record_attendance")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "Update the attendance status of users for an event.")
  public final Response recordEventAttendance(@Context final HttpServletRequest request,
                                              @PathParam("event_id") final String eventId,
                                              final List<Long> userIds,
                                              @QueryParam("attended") final Boolean attended) {
    List<Long> failedUserIds = new ArrayList<>();

    if (userIds.isEmpty()) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "No User Ids provided").toResponse();
    }

    try {
      RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);
      IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

      if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      for (Long userId : userIds) {
        recordAttendanceForUser(userId, event, attended, failedUserIds, currentUser, request);
      }

      if (failedUserIds.isEmpty()) {
        return Response.ok().build();
      } else {
        String errorMessage =
            String.format("One or more bookings could not be updated: %s", failedUserIds);
        return new SegueErrorResponse(Status.BAD_REQUEST, errorMessage).toResponse();
      }

    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      String errorMsg = "Database error occurred while trying to update an event booking";
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          EXCEPTION_MESSAGE_CONTENT_ERROR_RETRIEVING_BOOKING)
          .toResponse();
    } catch (EventIsCancelledException e) {
      return SegueErrorResponse.getBadRequestResponse("The event is cancelled: event attendance cannot be recorded.");
    }
  }

  private void recordAttendanceForUser(Long userId, IsaacEventPageDTO event, Boolean attended,
                                       List<Long> failedUserIds, RegisteredUserDTO currentUser,
                                       HttpServletRequest request) throws SegueDatabaseException,
                                        EventIsCancelledException {
    try {
      RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(userId);
      this.bookingManager.recordAttendance(event, userOfInterest, attended);
      this.getLogManager().logEvent(currentUser, request,
          SegueServerLogType.ADMIN_EVENT_ATTENDANCE_RECORDED,
          Map.of(
              EVENT_ID_FKEY_FIELDNAME, event.getId(),
              USER_ID_FKEY_FIELDNAME, userId,
              ATTENDED_FIELDNAME, attended,
              EVENT_DATE_FIELDNAME, event.getDate(),
              EVENT_TAGS_FIELDNAME, event.getTags()
          ));

      if (event.getTags().contains("teacher")) {
        this.userBadgeManager.updateBadge(userOfInterest,
            UserBadgeManager.Badge.TEACHER_CPD_EVENTS_ATTENDED, event.getId());
      }
    } catch (EventBookingUpdateException | NoUserException e) {
      failedUserIds.add(userId);
    }
  }

  /**
   * REST end point to provide a list of events.
   *
   * @param request    this allows us to check to see if a user is currently logged-in
   * @param startIndex the initial index for the first result
   * @param limit      the maximums number of results to return
   * @param filter     in which way should the results be filtered from a choice defined in the EventFilterOption enum
   * @return a Response containing a list of events objects or containing a SegueErrorResponse
   */
  @GET
  @Path("/overview")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "List summary information of events matching the provided criteria.")
  public final Response getEventOverviews(@Context final HttpServletRequest request,
                                          @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index")
                                          final Integer startIndex,
                                          @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit")
                                          final Integer limit,
                                          @QueryParam("filter") final String filter) {
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();

    final Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);

    fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));

    try {
      RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
      if (!Arrays.asList(Role.EVENT_LEADER, Role.EVENT_MANAGER, Role.ADMIN).contains(currentUser.getRole())) {
        return SegueErrorResponse.getIncorrectRoleResponse();
      }

      Map<String, AbstractFilterInstruction> filterInstructions = null;
      if (filter != null) {
        EventFilterOption filterOption = EventFilterOption.valueOf(filter);
        filterInstructions = Maps.newHashMap();
        if (filterOption.equals(EventFilterOption.FUTURE)) {
          DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(Instant.now(), null);
          filterInstructions.put(ENDDATE_FIELDNAME, anyEventsFromNow);
        } else if (filterOption.equals(EventFilterOption.RECENT)) {
          Instant oneMonthAgo = ZonedDateTime.now().minus(1L, ChronoUnit.MONTHS).toInstant();
          DateRangeFilterInstruction eventsOverPreviousMonth =
              new DateRangeFilterInstruction(oneMonthAgo, Instant.now());
          filterInstructions.put(ENDDATE_FIELDNAME, eventsOverPreviousMonth);
        } else if (filterOption.equals(EventFilterOption.PAST)) {
          DateRangeFilterInstruction anyEventsToNow = new DateRangeFilterInstruction(null, Instant.now());
          filterInstructions.put(ENDDATE_FIELDNAME, anyEventsToNow);
        }

      }

      ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
          ContentService.generateDefaultFieldToMatch(fieldsToMatch),
          startIndex, limit, sortInstructions, filterInstructions);

      // Get list of event IDs
      List<String> eventIds =
          findByFieldNames.getResults().stream().map(ContentDTO::getId).collect(Collectors.toList());

      // Get all bookings for multiple events in one go
      Map<String, List<DetailedEventBookingDTO>> allBookings = this.bookingManager.adminGetBookingsByEventIds(eventIds);

      List<Map<String, Object>> resultList = Lists.newArrayList();

      for (ContentDTO c : findByFieldNames.getResults()) {
        if (!(c instanceof IsaacEventPageDTO event)) {
          continue;
        }

        if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
          continue;
        }

        ImmutableMap.Builder<String, Object> eventOverviewBuilder = new ImmutableMap.Builder<>();
        eventOverviewBuilder.put(ID_FIELDNAME, event.getId());
        eventOverviewBuilder.put(TITLE_FIELDNAME, event.getTitle());
        eventOverviewBuilder.put("subtitle", event.getSubtitle());
        eventOverviewBuilder.put(DATE_FIELDNAME, event.getDate());
        eventOverviewBuilder.put("bookingDeadline",
            event.getBookingDeadline() == null ? event.getDate() : event.getBookingDeadline());
        eventOverviewBuilder.put("eventStatus", event.getEventStatus());
        eventOverviewBuilder.put("privateEvent", Objects.requireNonNullElse(event.isPrivateEvent(), false));

        if (null != event.getLocation()) {
          eventOverviewBuilder.put("location", event.getLocation());
        }

        // Use counts from batch query
        List<DetailedEventBookingDTO> bookingsForThisEvent = allBookings.getOrDefault(event.getId(), new ArrayList<>());
        long numberOfConfirmedBookings =
            bookingsForThisEvent.stream().filter(b -> BookingStatus.CONFIRMED.equals(b.getBookingStatus())).count();
        long numberOfReservedBookings =
            bookingsForThisEvent.stream().filter(b -> BookingStatus.RESERVED.equals(b.getBookingStatus())).count();
        long numberOfWaitingListBookings =
            bookingsForThisEvent.stream().filter(b -> BookingStatus.WAITING_LIST.equals(b.getBookingStatus())).count();
        long numberAttended =
            bookingsForThisEvent.stream().filter(b -> BookingStatus.ATTENDED.equals(b.getBookingStatus())).count();
        long numberAbsent =
            bookingsForThisEvent.stream().filter(b -> BookingStatus.ABSENT.equals(b.getBookingStatus())).count();

        eventOverviewBuilder.put("numberOfConfirmedBookings", numberOfConfirmedBookings);
        eventOverviewBuilder.put("numberOfReservedBookings", numberOfReservedBookings);
        eventOverviewBuilder.put("numberOfWaitingListBookings", numberOfWaitingListBookings);
        eventOverviewBuilder.put("numberAttended", numberAttended);
        eventOverviewBuilder.put("numberAbsent", numberAbsent);

        if (null != event.getNumberOfPlaces()) {
          eventOverviewBuilder.put("numberOfPlaces", event.getNumberOfPlaces());
        }

        resultList.add(eventOverviewBuilder.build());
      }

      return Response.ok(new ResultsWrapper<>(resultList, findByFieldNames.getTotalResults())).build();
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT)
          .toResponse();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      log.error("Error occurred during event overview look up", e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the database content you requested.")
          .toResponse();
    } catch (IllegalArgumentException e) {
      log.error("Error occurred during event overview look up", e);
      return new SegueErrorResponse(Status.BAD_REQUEST, "Invalid request format.").toResponse();
    }
  }

  /**
   * REST end point to provide a summary of events suitable for mapping.
   *
   * @param request        this allows us to check to see if a user is currently logged in.
   * @param tags           a string of comma-separated tags for use in filtering the search
   * @param startIndex     the initial index for the first result.
   * @param limit          the maximums number of results to return
   * @param showActiveOnly true will impose filtering on the results. False will not. Defaults to false.
   * @param showStageOnly  if present, only events with an audience matching this string will be shown
   * @return a Response containing a list of event map summaries or containing a SegueErrorResponse.
   */
  @GET
  @Path("/map_data")
  @Produces(MediaType.APPLICATION_JSON)
  @GZIP
  @Operation(summary = "List summary details suitable for mapping for events matching the provided criteria.")
  public final Response getEventMapData(@Context final HttpServletRequest request,
                                        @QueryParam("tags") final String tags,
                                        @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index")
                                        final Integer startIndex,
                                        @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit")
                                        final Integer limit,
                                        @QueryParam("show_active_only") final Boolean showActiveOnly,
                                        @QueryParam("show_stage_only") final String showStageOnly) {
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();

    Integer newLimit = null;
    Integer newStartIndex = null;
    if (limit != null) {
      newLimit = limit;
    }

    if (startIndex != null) {
      newStartIndex = startIndex;
    }

    if (tags != null) {
      fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
    }

    if (showStageOnly != null) {
      fieldsToMatch.put(STAGE_FIELDNAME, Arrays.asList(showStageOnly.split(",")));
    }

    final Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);

    fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));

    Map<String, AbstractFilterInstruction> filterInstructions = null;
    if (null == showActiveOnly || showActiveOnly) {
      filterInstructions = Maps.newHashMap();
      DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(Instant.now(), null);
      filterInstructions.put(ENDDATE_FIELDNAME, anyEventsFromNow);
      sortInstructions.put(DATE_FIELDNAME, SortOrder.ASC);
    }

    try {
      ResultsWrapper<ContentDTO> findByFieldNames = null;

      findByFieldNames = this.contentManager.findByFieldNames(
          ContentService.generateDefaultFieldToMatch(fieldsToMatch),
          newStartIndex, newLimit, sortInstructions, filterInstructions);

      List<Map<String, Object>> resultList = Lists.newArrayList();

      for (ContentDTO c : findByFieldNames.getResults()) {
        if (!(c instanceof IsaacEventPageDTO e)) {
          continue;
        }

        if (null == e.getLocation() || null == e.getLocation().getLatitude() && null == e.getLocation()
            .getLongitude()) {
          // Ignore events without locations.
          continue;
        }
        if (e.getLocation().getLatitude().equals(0.0) && e.getLocation().getLongitude().equals(0.0)) {
          // Ignore events with locations that haven't been set properly.
          log.info("Event with 0.0 lat/long:  {}", e.getId());
          continue;
        }

        ImmutableMap.Builder<String, Object> eventOverviewBuilder = new ImmutableMap.Builder<>();
        eventOverviewBuilder.put(ID_FIELDNAME, e.getId());
        eventOverviewBuilder.put(TITLE_FIELDNAME, e.getTitle());
        eventOverviewBuilder.put(DATE_FIELDNAME, e.getDate());
        eventOverviewBuilder.put("subtitle", e.getSubtitle());
        if (e.getEventStatus() != null) {
          eventOverviewBuilder.put("status", e.getEventStatus());
        }
        // The schema required needs lat and long at top-level, so add address at top-level too.
        eventOverviewBuilder.put("address", e.getLocation().getAddress());
        eventOverviewBuilder.put("latitude", e.getLocation().getLatitude());
        eventOverviewBuilder.put("longitude", e.getLocation().getLongitude());

        if (null != e.getBookingDeadline()) {
          eventOverviewBuilder.put("deadline", e.getBookingDeadline());
        }

        resultList.add(eventOverviewBuilder.build());
      }

      return Response.ok(new ResultsWrapper<>(resultList, findByFieldNames.getTotalResults())).build();
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT)
          .toResponse();
    }
  }

  /**
   * A helper method for retrieving an event object without augmented information.
   *
   * @param eventId the id of the event of interest
   * @return the fully populated event dto with user context information.
   * @throws ContentManagerException if there is a problem finding the event information
   * @throws SegueDatabaseException  if there is a database error.
   */
  private IsaacEventPageDTO getRawEventDTOById(final String eventId)
      throws ContentManagerException, SegueDatabaseException {

    ContentDTO possibleEvent = this.contentManager.getContentById(eventId);

    if (null == possibleEvent) {
      throw new ResourceNotFoundException(String.format("Unable to locate the event with id; %s", eventId));
    }

    if (possibleEvent instanceof IsaacEventPageDTO definiteEvent) {
      // The Events Facade *mutates* the EventDTO returned by this method; we must return a copy of
      // the original object else we will poison the contentManager's cache!
      return mapper.copy(definiteEvent);
    }
    return null;
  }

  /**
   * A helper method for retrieving an event and the number of places available and if the user is booked or not.
   * <br>
   *
   * @param request so we can determine if the user is logged in
   * @param eventId the id of the event of interest
   * @return the fully populated event dto with user context information.
   * @throws ContentManagerException if there is a problem finding the event information
   * @throws SegueDatabaseException  if there is a database error.
   */
  private IsaacEventPageDTO getAugmentedEventDTOById(final HttpServletRequest request, final String eventId)
      throws ContentManagerException, SegueDatabaseException {
    IsaacEventPageDTO event = getRawEventDTOById(eventId);
    return augmentEventWithBookingInformation(request, event);
  }

  /**
   * Augment a single event with booking information before we send it out.
   *
   * @param request       for user look up
   * @param possibleEvent a ContentDTO that should hopefully be an IsaacEventPageDTO.
   * @return an augmented IsaacEventPageDTO.
   * @throws SegueDatabaseException if an error occurs while retrieving booking information
   */
  private IsaacEventPageDTO augmentEventWithBookingInformation(final HttpServletRequest request,
                                                               final ContentDTO possibleEvent)
      throws SegueDatabaseException {
    if (possibleEvent instanceof IsaacEventPageDTO page) {

      try {
        RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
        BookingStatus userBookingStatus = this.bookingManager.getBookingStatus(page.getId(), user.getId());
        page.setUserBookingStatus(userBookingStatus);

        if (!isUserBookingConfirmedAndEventToday(userBookingStatus, page.getDate())) {
          page.setMeetingUrl(null);
        }

      } catch (NoUserLoggedInException e) {
        // no action as we don't require the user to be logged in.
        page.setUserBookingStatus(null);
        page.setMeetingUrl(null);
      }
      page.setPlacesAvailable(this.bookingManager.getPlacesAvailable(page));
      return page;
    } else {
      throw new ClassCastException("The object provided was not an event.");
    }
  }

  /**
   * A helper method used to determine if the current user booking is confirmed and date of event is today.
   *
   * @param userBookingStatus status of the current user's booking
   * @param date              start date of event from IsaacEventPageDTO.
   * @return boolean.
   */

  private boolean isUserBookingConfirmedAndEventToday(final BookingStatus userBookingStatus, final Instant date) {

    if (date == null) {
      return false;
    }

    LocalDate today = LocalDate.now();
    LocalDate eventStartDate = date.atZone(ZoneId.systemDefault()).toLocalDate();

    return userBookingStatus == BookingStatus.CONFIRMED && eventStartDate.isEqual(today);
  }
}
