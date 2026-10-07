package com.geopslabs.geops.engagement.application.usecases;

public record SaveOfferResult(SavedOfferView savedOffer, boolean created) {

    public static SaveOfferResult created(SavedOfferView savedOffer) {
        return new SaveOfferResult(savedOffer, true);
    }

    public static SaveOfferResult existing(SavedOfferView savedOffer) {
        return new SaveOfferResult(savedOffer, false);
    }
}
