package com.geopslabs.geops.reservation.domain.models.commands;

/**
 * CreateReservationCommand
 *
 * Command record that encapsulates all the necessary data to create a new reservation.
 * This command validates input data and ensures that required fields are properly provided
 * for reservation creation, supporting reservation generation from payments and offer redemption.
 *
 * @summary Command to create a new reservation
 * @param userId The unique identifier of the user who owns the reservation
 * @param paymentId The payment identifier that generated this reservation
 * @param paymentCode The payment code generated at payment time
 * @param productType The product type copied from payment (optional)
 * @param offerId The reference to the offer id (optional)
 * @param code The reservation code to redeem
 * @param expiresAt The expiration date of the reservation (optional)
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record CreateReservationCommand(
    Long userId,
    Long paymentId,
    String paymentCode,
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
    public CreateReservationCommand {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId cannot be null or empty");
        }

        if (paymentCode == null || paymentCode.isBlank()) {
            throw new IllegalArgumentException("paymentCode cannot be null or empty");
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }

        // Validate offer ID if provided
        if (offerId != null && offerId <= 0) {
            throw new IllegalArgumentException("offerId must be positive if provided");
        }
    }
}
