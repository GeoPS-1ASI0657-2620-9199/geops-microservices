package com.geopslabs.geops.reservation.domain.models.queries;

import com.geopslabs.geops.reservation.domain.models.ReservationStatus;

public record GetReservationsByConsumerIdQuery(Long consumerId, ReservationStatus status) {
}
