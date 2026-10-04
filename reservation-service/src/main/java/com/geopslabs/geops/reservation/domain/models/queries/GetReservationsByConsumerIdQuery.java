package com.geopslabs.geops.reservation.domain.models.queries;

public record GetReservationsByConsumerIdQuery(Long consumerId) {
    public GetReservationsByConsumerIdQuery {
        if (consumerId == null || consumerId <= 0) {
            throw new IllegalArgumentException("consumerId cannot be null or empty");
        }
    }
}
