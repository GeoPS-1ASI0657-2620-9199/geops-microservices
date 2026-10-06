package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class ReservationResponseAssembler {

    private ReservationResponseAssembler() {
    }

    public static ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getCode().value(),
                reservation.getConsumerId(), reservation.getOfferId(), reservation.getBusinessId(),
                reservation.getOfferTitle(), reservation.getStatus().name(), toInstant(reservation.getReservedAt()),
                toInstant(reservation.getExpiresAt()), toInstant(reservation.getRedeemedAt()));
    }

    public static CreatedReservationResponse toCreatedResponse(Reservation reservation) {
        return new CreatedReservationResponse(reservation.getId(), reservation.getCode().value(),
                toInstant(reservation.getExpiresAt()));
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
