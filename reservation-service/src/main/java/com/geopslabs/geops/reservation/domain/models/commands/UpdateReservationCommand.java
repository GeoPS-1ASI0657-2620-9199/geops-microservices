package com.geopslabs.geops.reservation.domain.models.commands;

/**
 * UpdateReservationCommand
 *
 * Command record for updating an existing reservation.
 * This command allows partial updates of reservation data, including product type,
 * offer reference, reservation code, and expiration date.
 *
 * @summary Command to update an existing reservation
 * @param reservationId The unique identifier of the reservation to update
 * @param productType Updated product type (optional)
 * @param offerId Updated reference to the offer id (optional)
 * @param code Updated reservation code to redeem (optional)
 * @param expiresAt Updated expiration date (optional)
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record UpdateReservationCommand(
    Long reservationId,
    String productType,
    Long offerId,
    String code,
    String expiresAt
) {
    /**
     * Compact constructor that validates the command parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public UpdateReservationCommand {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("reservationId cannot be null or negative");
        }

        // Validate optional fields if provided
        if (code != null && code.isBlank()) {
            throw new IllegalArgumentException("code cannot be empty string if provided");
        }

        if (offerId != null && offerId <= 0) {
            throw new IllegalArgumentException("offerId must be positive if provided");
        }

        if (productType != null && productType.isBlank()) {
            throw new IllegalArgumentException("productType cannot be empty string if provided");
        }
    }
}
