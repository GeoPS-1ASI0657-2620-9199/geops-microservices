package com.geopslabs.geops.reservation.infrastructure.web;

public record ReservationResponse(
    Long id,
    Long consumerId,
    Long offerId,
    String code,
    String expiresAt
) {
}
