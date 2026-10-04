package com.geopslabs.geops.reservation.infrastructure.web;

public record CreateReservationRequest(
    Long consumerId,
    Long offerId,
    String code,
    String expiresAt
) {
    public CreateReservationRequest {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null or empty");
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }

        if (offerId != null && offerId <= 0) {
            throw new IllegalArgumentException("offerId must be positive if provided");
        }
    }
}
