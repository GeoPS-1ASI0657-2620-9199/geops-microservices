package com.geopslabs.geops.reservation.domain.models.queries;

/**
 * GetReservationByCodeQuery
 *
 * Query record to retrieve a reservation by its redemption code.
 * This query is essential for reservation redemption processes where users
 * provide a reservation code to redeem their benefits.
 *
 * @summary Query to retrieve a reservation by its code
 * @param code The reservation redemption code
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetReservationByCodeQuery(String code) {
    /**
     * Compact constructor that validates the query parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public GetReservationByCodeQuery {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }
    }
}
