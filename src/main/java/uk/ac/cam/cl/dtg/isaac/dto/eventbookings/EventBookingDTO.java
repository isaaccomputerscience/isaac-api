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

package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import java.time.Instant;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

/**
 * Event Booking object.
 *
 * @author sac92
 *
 */
public class EventBookingDTO {
  private Long bookingId;

  private UserSummaryDTO userBooked;

  private Long reservedById;

  private String eventId;

  private String eventTitle;

  private Instant eventDate;

  private BookingStatus bookingStatus;

  private Instant lastUpdated;

  private Instant bookingDate;

  /**
   * EventBookingDTO.
   */
  public EventBookingDTO() {

  }

  /**
   * Constructor for EventBookingDTO.
   *
   * @param bookingId
   *            - id of the booking
   * @param userBooked
   *            - the user summary of the user booked on the event.
   * @param reservedById
   *            - the id of the user making the reservation
   * @param eventId
   *            - the event id
   * @param eventTitle
   *            - event title
   * @param eventDate
   *            - date of the event
   * @param bookingDate
   *            - booking date.
   * @param lastUpdated
   *            - the date the booking was last updated
   * @param status
   *            - the booking status
   */
  public EventBookingDTO(final Long bookingId, final UserSummaryDTO userBooked, final Long reservedById,
                         final String eventId, final String eventTitle, final Instant eventDate,
                         final Instant bookingDate, final Instant lastUpdated, final BookingStatus status) {
    this.bookingId = bookingId;
    this.userBooked = userBooked;
    this.reservedById = reservedById;
    this.eventId = eventId;
    this.eventTitle = eventTitle;
    this.eventDate = eventDate;
    this.bookingStatus = status;
    this.lastUpdated = lastUpdated;
    this.bookingDate = bookingDate;
  }

  /**
   * Gets the bookingId.
   *
   * @return the bookingId
   */
  public Long getBookingId() {
    return bookingId;
  }

  /**
   * Sets the bookingId.
   *
   * @param bookingId
   *            the bookingId to set
   */
  public void setBookingId(final Long bookingId) {
    this.bookingId = bookingId;
  }

  /**
   * Gets the userBooked.
   *
   * @return the userBooked
   */
  public UserSummaryDTO getUserBooked() {
    return userBooked;
  }

  /**
   * Sets the userBooked.
   *
   * @param userBooked
   *            the userBooked to set
   */
  public void setUserBooked(final UserSummaryDTO userBooked) {
    this.userBooked = userBooked;
  }

  /**
   * Gets the user who created the reservation.
   *
   * @return the user who created the reservation.
   */
  public Long getReservedById() {
    return reservedById;
  }

  /**
   * Sets the user who created the reservation.
   *
   * @param reservedById - the user who created the reservation.
   */
  public void setReservedById(final Long reservedById) {
    this.reservedById = reservedById;
  }

  /**
   * Gets the eventId.
   *
   * @return the eventId
   */
  public String getEventId() {
    return eventId;
  }

  /**
   * Sets the eventId.
   *
   * @param eventId
   *            the eventId to set
   */
  public void setEventId(final String eventId) {
    this.eventId = eventId;
  }

  /**
   * Gets the eventTitle.
   *
   * @return the eventTitle
   */
  public String getEventTitle() {
    return eventTitle;
  }

  /**
   * Sets the eventTitle.
   *
   * @param eventTitle
   *            the eventTitle to set
   */
  public void setEventTitle(final String eventTitle) {
    this.eventTitle = eventTitle;
  }

  /**
   * Gets the eventDate.
   *
   * @return the eventDate
   */
  public Instant getEventDate() {
    return eventDate;
  }

  /**
   * Sets the eventDate.
   *
   * @param eventDate
   *            the eventDate to set
   */
  public void setEventDate(final Instant eventDate) {
    this.eventDate = eventDate;
  }

  /**
   * Gets the bookingDate.
   *
   * @return the bookingDate
   */
  public Instant getBookingDate() {
    return bookingDate;
  }

  /**
   * Sets the bookingDate.
   *
   * @param bookingDate
   *            the bookingDate to set
   */
  public void setBookingDate(final Instant bookingDate) {
    this.bookingDate = bookingDate;
  }

  /**
   * Get the lastUpdate date.
   *
   * @return the Date it was most recently updated
   */
  public Instant getUpdated() {
    return lastUpdated;
  }

  /**
   * Set the lastUpdate Date.
   *
   * @param lastUpdated - date it was updated.
   */
  public void setUpdated(final Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  /**
   * Get the booking status.
   *
   * @return the status of the booking.
   */
  public BookingStatus getBookingStatus() {
    return bookingStatus;
  }

  /**
   * Set the status of the booking.
   *
   * @param bookingStatus - the status to set.
   */
  public void setBookingStatus(final BookingStatus bookingStatus) {
    this.bookingStatus = bookingStatus;
  }
}
