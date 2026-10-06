package com.geopslabs.geops.engagement.infrastructure.web;

/**
 * SavedOfferResponse
 *
 * Resource Resource for representing saved offer data via REST API
 * This resource encapsulates the saved offer information returned in API responses
 *
 * @summary Resource for saved offer representation
 * @param id The unique identifier of the saved offer
 * @param consumerId The ID of the user who created the saved offer
 * @param offerId The ID of the saved offer offer
 * @param createdAt The timestamp when the saved offer was created
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record SavedOfferResponse(
        Long id,
        Long consumerId,
        Long offerId,
        String savedAt
) {
}
