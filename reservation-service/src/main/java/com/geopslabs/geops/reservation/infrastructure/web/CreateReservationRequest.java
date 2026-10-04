package com.geopslabs.geops.reservation.infrastructure.web;

/**
 * CreateReservationRequest
 *
 * Resource Resource for creating reservations via REST API.
 * This resource represents the request payload for reservation creation,
 * containing all necessary information for setting up a new reservation.
 * Based on the frontend Reservation entity structure.
 *
 * @summary Request resource for creating reservations
 * @param consumerId The unique identifier of the user who owns the reservation
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
public record CreateReservationRequest(
    Long consumerId,
    Long paymentId,
    String paymentCode,
    String productType,
    Long offerId,
    String code,
    String expiresAt
) {
    /**
     * Compact constructor that validates the resource parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public CreateReservationRequest {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null or empty");
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

        if (offerId != null && offerId <= 0) {
            throw new IllegalArgumentException("offerId must be positive if provided");
        }
    }
}
