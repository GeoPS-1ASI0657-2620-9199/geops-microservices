package com.geopslabs.geops.engagement.infrastructure.web;

/**
 * SaveOfferRequest
 *
 * Resource representing the data required to create a new saved offer offer for a user
 *
 * @summary Resource for creating a saved offer offer
 * @since 1.0
 * @author GeOps Labs
 */
public record SaveOfferRequest(
    Long consumerId,
    Long offerId
) {
    /**
     * Compact constructor that validates the resource parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public SaveOfferRequest {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null");
        }

        if (offerId == null) {
            throw new IllegalArgumentException("offerId cannot be null");
        }
    }
}

