package com.geopslabs.geops.engagement.domain.models.queries;

/**
 * GetSavedOfferByIdQuery
 *
 * Query record to retrieve a saved offer by its ID.
 * Useful for validation before deletion.
 *
 * @summary Query to retrieve a saved offer by ID
 * @param id The saved offer unique identifier
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record GetSavedOfferByIdQuery(Long id) {
    public GetSavedOfferByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
    }
}

