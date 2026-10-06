package com.geopslabs.geops.engagement.domain.models.queries;

public record GetSavedOffersByConsumerQuery(Long consumerId) {
    public GetSavedOffersByConsumerQuery {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null or empty");
        }
    }
}
