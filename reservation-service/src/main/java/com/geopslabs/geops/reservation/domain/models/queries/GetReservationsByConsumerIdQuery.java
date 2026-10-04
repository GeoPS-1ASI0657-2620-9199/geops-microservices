package com.geopslabs.geops.reservation.domain.models.queries;

/**
 * GetReservationsByConsumerIdQuery
 *
 * Query record to retrieve all reservations for a specific user.
 * This query helps in displaying user reservation history, tracking user reservations,
 * and providing comprehensive reservation information for a particular user.
 *
 * @summary Query to retrieve all reservations for a specific user
 * @param consumerId The unique identifier of the user whose reservations to retrieve
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetReservationsByConsumerIdQuery(Long consumerId) {
    /**
     * Compact constructor that validates the query parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public GetReservationsByConsumerIdQuery {
        if (consumerId == null || consumerId <= 0) {
            throw new IllegalArgumentException("consumerId cannot be null or empty");
        }
    }
}
