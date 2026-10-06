package com.geopslabs.geops.engagement.domain.models.queries;

public record GetReviewsByBusinessQuery(Long businessId) {
    public GetReviewsByBusinessQuery {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null or empty");
        }
    }
}

