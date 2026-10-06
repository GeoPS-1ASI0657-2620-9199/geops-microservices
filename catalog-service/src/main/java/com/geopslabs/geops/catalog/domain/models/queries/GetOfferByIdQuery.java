package com.geopslabs.geops.catalog.domain.models.queries;

public record GetOfferByIdQuery(Long id) {
    public GetOfferByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
    }
}

