package com.geopslabs.geops.reservation.domain.models.queries;

public record GetReservationByCodeQuery(String code) {
    public GetReservationByCodeQuery {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }
    }
}
