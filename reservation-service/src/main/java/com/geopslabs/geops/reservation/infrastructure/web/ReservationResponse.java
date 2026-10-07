package com.geopslabs.geops.reservation.infrastructure.web;

import java.time.Instant;

public record ReservationResponse(
        Long reservationId,
        String code,
        Long consumerId,
        Long offerId,
        Long businessId,
        String offerTitle,
        String status,
        Instant reservedAt,
        Instant expiresAt,
        Instant redeemedAt) {
}
