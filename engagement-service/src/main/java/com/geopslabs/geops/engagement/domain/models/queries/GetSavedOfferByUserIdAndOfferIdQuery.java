package com.geopslabs.geops.engagement.domain.models.queries;

/**
 * GetSavedOfferByUserIdAndOfferIdQuery
 *
 * Query record to retrieve a saved offer by user ID and offer ID
 * This query is useful for checking if a specific offer is marked as saved offer by a user
 *
 * @summary Query to retrieve a saved offer by user ID and offer ID
 * @param userId  The unique identifier of the user
 * @param offerId The unique identifier of the offer
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetSavedOfferByUserIdAndOfferIdQuery(Long userId, Long offerId) {
    /**
     * Compact constructor that validates the query parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public GetSavedOfferByUserIdAndOfferIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        if (offerId == null) {
            throw new IllegalArgumentException("offerId cannot be null");
        }
    }
}

