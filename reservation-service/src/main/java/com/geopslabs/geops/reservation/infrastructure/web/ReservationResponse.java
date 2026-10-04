package com.geopslabs.geops.reservation.infrastructure.web;

/**
 * ReservationResponse
 *
 * Resource Resource for reservation responses via REST API.
 * This resource represents the response payload containing reservation information
 * when retrieving reservation data from the system.
 * Based on the frontend Reservation entity structure.
 *
 * @summary Response resource for reservation data
 * @param id The unique identifier of the reservation
 * @param consumerId The unique identifier of the user who owns the reservation
 * @param paymentId The payment identifier that generated this reservation
 * @param paymentCode The payment code generated at payment time
 * @param productType The product type copied from payment (optional)
 * @param offerId The reference to the offer id (optional)
 * @param offer The optional embedded offer data (when requested)
 * @param code The reservation code to redeem
 * @param expiresAt The expiration date of the reservation (optional)
 * @param createdAt Timestamp when the reservation was created
 * @param updatedAt Timestamp when the reservation was last updated
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record ReservationResponse(
    Long id,
    Long consumerId,
    Long paymentId,
    String paymentCode,
    String productType,
    Long offerId,
    String code,
    String expiresAt
) {
    // This record doesn't need validation in the compact constructor
    // as it's used for response data that should already be validated
}
