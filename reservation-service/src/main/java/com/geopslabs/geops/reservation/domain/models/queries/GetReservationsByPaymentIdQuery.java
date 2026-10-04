package com.geopslabs.geops.reservation.domain.models.queries;

/**
 * GetReservationsByPaymentIdQuery
 *
 * Query record to retrieve all reservations generated from a specific payment.
 * This query is useful for tracking reservations generated from a particular payment
 * and understanding the relationship between payments and reservations.
 *
 * @summary Query to retrieve reservations by payment ID
 * @param paymentId The unique identifier of the payment that generated the reservations
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetReservationsByPaymentIdQuery(Long paymentId) {
    /**
     * Compact constructor that validates the query parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public GetReservationsByPaymentIdQuery {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId cannot be null or empty");
        }
    }
}
