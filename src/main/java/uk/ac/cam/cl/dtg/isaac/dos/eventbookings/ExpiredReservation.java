package uk.ac.cam.cl.dtg.isaac.dos.eventbookings;

import jakarta.annotation.Nullable;
import java.time.Instant;

/**
 * A RESERVED booking that has just been cancelled because its reservation close date passed.
 *
 * @param eventId              the event the reservation was for
 * @param userId               the student the reservation was held for
 * @param reservedById         the teacher who made the reservation, if known
 * @param reservationCloseDate when the reservation was due to lapse; equals the event start date for reservations
 *                             made less than the full confirmation window before the event
 */
public record ExpiredReservation(String eventId, Long userId, @Nullable Long reservedById,
                                 Instant reservationCloseDate) {
}
