package com.geopslabs.geops.reservation.domain.models.queries;

/**
 * GetReservationByIdQuery
 *
 * Query record to retrieve a reservation by its unique identifier.
 * This query is used to fetch specific reservation details when needed
 * for validation, redemption, or displaying reservation information.
 *
 * @summary Query to retrieve a reservation by its ID
 * @param reservationId The unique identifier of the reservation
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetReservationByIdQuery(Long reservationId) {
    /**
     * Compact constructor that validates the query parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public GetReservationByIdQuery {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("reservationId cannot be null or negative");
        }
    }
}
