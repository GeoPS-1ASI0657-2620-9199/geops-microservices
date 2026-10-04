package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.Reservation;

public class ReservationResponseAssembler {

    public static ReservationResponse toResourceFromEntity(Reservation entity) {
        return new ReservationResponse(
            entity.getId(),
            entity.getConsumerId(),
            entity.getOfferId(),
            entity.getCode(),
            entity.getExpiresAt() != null ? entity.getExpiresAt().toString() : null
        );
    }
}
