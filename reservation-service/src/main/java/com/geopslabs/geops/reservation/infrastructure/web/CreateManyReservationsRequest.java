package com.geopslabs.geops.reservation.infrastructure.web;

import java.util.List;

/**
 * CreateManyReservationsRequest
 *
 * Resource Resource for creating multiple reservations via REST API.
 * This resource represents the request payload for bulk reservation creation,
 * supporting the frontend's createMany method that expects a bulk endpoint.
 *
 * @summary Request resource for creating multiple reservations
 * @param reservations List of CreateReservationRequest objects to create
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record CreateManyReservationsRequest(
    List<CreateReservationRequest> reservations
) {
    /**
     * Compact constructor that validates the resource parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public CreateManyReservationsRequest {
        if (reservations == null || reservations.isEmpty()) {
            throw new IllegalArgumentException("reservations list cannot be null or empty");
        }

        if (reservations.size() > 100) {
            throw new IllegalArgumentException("cannot create more than 100 reservations at once");
        }
    }
}
