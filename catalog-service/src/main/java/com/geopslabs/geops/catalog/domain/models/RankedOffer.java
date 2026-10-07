package com.geopslabs.geops.catalog.domain.models;

public record RankedOffer(NearbyOfferCandidate offer, double distanceMeters, int walkMinutes) {

    public static RankedOffer of(NearbyOfferCandidate offer, double distanceMeters) {
        return new RankedOffer(offer, distanceMeters, WalkingRadius.walkMinutesFor(distanceMeters));
    }
}
