package com.geopslabs.geops.reservation.domain.models.queries;

/**
 * GetReservationsByConsumerIdQuery
 *
 * Query record to retrieve all reservations for a specific user.
 * This query helps in displaying user reservation history, tracking user reservations,
 * and providing comprehensive reservation information for a particular user.
 *
 * @summary Query to retrieve all reservations for a specific user
 * @param userId The unique identifier of the user whose reservations to retrieve
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetReservationsByConsumerIdQuery(String userId) {
    /**
     * Compact constructor that validates the query parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public GetReservationsByConsumerIdQuery {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
    }
}
