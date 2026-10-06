package com.geopslabs.geops.catalog.domain.models.queries;

import java.util.List;

public record GetOffersByIdsQuery(List<Long> ids) {
    public GetOffersByIdsQuery {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids list cannot be null or empty");
        }

        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("all ids must be positive");
        }
    }
}
