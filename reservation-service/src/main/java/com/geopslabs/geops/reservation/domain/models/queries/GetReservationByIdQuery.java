package com.geopslabs.geops.reservation.domain.models.queries;

public record GetReservationByIdQuery(Long reservationId) {
    public GetReservationByIdQuery {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("reservationId cannot be null or negative");
        }
    }
}
