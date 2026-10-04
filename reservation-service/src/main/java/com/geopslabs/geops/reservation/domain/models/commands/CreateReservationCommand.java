package com.geopslabs.geops.reservation.domain.models.commands;

public record CreateReservationCommand(
    Long consumerId,
    Long offerId,
    String code,
    String expiresAt
) {
    public CreateReservationCommand {
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
