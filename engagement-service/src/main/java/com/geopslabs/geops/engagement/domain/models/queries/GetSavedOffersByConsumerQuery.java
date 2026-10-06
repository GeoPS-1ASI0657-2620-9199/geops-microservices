package com.geopslabs.geops.engagement.domain.models.queries;

/**
 * GetSavedOffersByConsumerQuery
 *
 * Query record to retrieve all saved offer items associated with a specific user
 * This query is used to fetch user saved offer items for display or processing
 *
 * @summary Query to retrieve saved offers by user ID
 * @param userId The unique identifier of the user
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetSavedOffersByConsumerQuery(Long userId) {
    /**
     * Compact constructor that validates the query parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public GetSavedOffersByConsumerQuery {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
    }
}
