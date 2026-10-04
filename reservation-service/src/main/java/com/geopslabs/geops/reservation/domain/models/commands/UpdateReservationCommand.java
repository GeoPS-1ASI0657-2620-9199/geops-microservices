package com.geopslabs.geops.reservation.domain.models.commands;

public record UpdateReservationCommand(
    Long reservationId,
    Long offerId,
    String code,
    String expiresAt
) {
    public UpdateReservationCommand {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("reservationId cannot be null or negative");
        }

        if (code != null && code.isBlank()) {
            throw new IllegalArgumentException("code cannot be empty string if provided");
        }

        if (offerId != null && offerId <= 0) {
            throw new IllegalArgumentException("offerId must be positive if provided");
        }
    }
}
