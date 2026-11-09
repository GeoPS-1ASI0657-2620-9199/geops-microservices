package com.geopslabs.geops.reservation.infrastructure.web;

import java.time.Instant;

public record RedeemedReservationResponse(
        Long reservationId,
        String code,
        Long offerId,
        String offerTitle,
        String status,
        Instant redeemedAt) {
}
