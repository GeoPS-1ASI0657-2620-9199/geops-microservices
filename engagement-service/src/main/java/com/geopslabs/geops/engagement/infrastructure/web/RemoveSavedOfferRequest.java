package com.geopslabs.geops.engagement.infrastructure.web;

/**
 * RemoveSavedOfferRequest
 *
 * Resource representing the data required to delete a saved offer by user and offer
 *
 * @summary Resource for deleting a saved offer by userId and offerId
 * @since 1.0
 * @author GeOps Labs
 */
public record RemoveSavedOfferRequest(
    Long userId,
    Long offerId
) {
    /**
     * Compact constructor that validates the resource parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public RemoveSavedOfferRequest {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        if (offerId == null) {
            throw new IllegalArgumentException("offerId cannot be null");
        }
    }
}

