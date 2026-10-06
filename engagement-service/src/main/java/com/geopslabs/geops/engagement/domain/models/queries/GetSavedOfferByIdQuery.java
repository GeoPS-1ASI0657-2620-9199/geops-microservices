package com.geopslabs.geops.engagement.domain.models.queries;

public record GetSavedOfferByIdQuery(Long id) {
    public GetSavedOfferByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
    }
}

