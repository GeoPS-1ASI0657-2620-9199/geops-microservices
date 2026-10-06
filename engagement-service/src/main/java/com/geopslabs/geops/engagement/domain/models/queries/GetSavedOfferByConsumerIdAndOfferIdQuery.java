package com.geopslabs.geops.engagement.domain.models.queries;

public record GetSavedOfferByConsumerIdAndOfferIdQuery(Long consumerId, Long offerId) {
    public GetSavedOfferByConsumerIdAndOfferIdQuery {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null");
        }

        if (offerId == null) {
            throw new IllegalArgumentException("offerId cannot be null");
        }
    }
}

