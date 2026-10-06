package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class ProximitySearchService {
    private static final Comparator<RankedOffer> BY_DISTANCE = Comparator.comparingDouble(RankedOffer::distanceMeters);

    private final OfferRepositoryPort offers;
    private final Clock clock;

    public ProximitySearchService(OfferRepositoryPort offers, Clock clock) {
        this.offers = offers;
        this.clock = clock;
    }

    public List<RankedOffer> search(GeoPoint origin, int radiusMeters, String category) {
        var today = LocalDate.now(clock);
        return offers.findPublishedWithin(origin, radiusMeters, today).stream()
                .filter(candidate -> category == null || category.equals(candidate.category()))
                .map(candidate -> RankedOffer.of(candidate, origin.distanceTo(candidate.location())))
                .filter(ranked -> ranked.distanceMeters() <= radiusMeters)
                .sorted(BY_DISTANCE)
                .toList();
    }
}
