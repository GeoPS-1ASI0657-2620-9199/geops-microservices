package com.geopslabs.geops.engagement.infrastructure.web;

public record RemoveSavedOfferRequest(
    Long consumerId,
    Long offerId
) {
    public RemoveSavedOfferRequest {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null");
        }

        if (offerId == null) {
            throw new IllegalArgumentException("offerId cannot be null");
        }
    }
}

