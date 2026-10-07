package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.application.usecases.SavedOfferView;

import java.time.ZoneOffset;

public final class SavedOfferResponseAssembler {

    private SavedOfferResponseAssembler() {
    }

    public static SavedOfferResponse toResponse(SavedOfferView view) {
        var savedOffer = view.savedOffer();
        var offer = view.offer();
        return new SavedOfferResponse(savedOffer.getId(), offer.offerId(), offer.businessId(), view.businessName(),
                offer.title(), offer.validTo(), view.expired(), savedOffer.getSavedAt().toInstant(ZoneOffset.UTC));
    }
}
