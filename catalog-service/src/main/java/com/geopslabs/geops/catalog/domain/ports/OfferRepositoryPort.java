package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.NearbyOfferCandidate;
import com.geopslabs.geops.catalog.domain.models.Offer;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OfferRepositoryPort {
    Offer save(Offer offer);

    Optional<Offer> findById(Long id);

    List<Offer> findByCampaignId(Long campaignId);

    List<NearbyOfferCandidate> findPublishedWithin(GeoPoint origin, int radiusMeters, LocalDate today);
}
