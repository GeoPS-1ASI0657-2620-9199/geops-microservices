package com.geopslabs.geops.reservation.infrastructure.web;

import java.time.Instant;

public record CreatedReservationResponse(Long reservationId, String code, Instant expiresAt) {
}
