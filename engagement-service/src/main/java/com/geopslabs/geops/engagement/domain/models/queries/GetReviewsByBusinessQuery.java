package com.geopslabs.geops.engagement.domain.models.queries;

/**
 * GetReviewsByBusinessQuery
 *
 * Query record to retrieve all reviews associated with a specific business.
 * This query is used to fetch consumer feedback for a business, which can be
 * displayed next to its offers
 *
 * @summary Query to retrieve reviews by business ID
 * @param businessId The unique identifier of the business
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetReviewsByBusinessQuery(Long businessId) {
    /**
     * Compact constructor that validates the query parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public GetReviewsByBusinessQuery {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null or empty");
        }
    }
}

