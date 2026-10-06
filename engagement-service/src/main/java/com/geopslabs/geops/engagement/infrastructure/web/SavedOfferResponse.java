package com.geopslabs.geops.engagement.infrastructure.web;

public record SavedOfferResponse(
        Long id,
        Long consumerId,
        Long offerId,
        String savedAt
) {
}
