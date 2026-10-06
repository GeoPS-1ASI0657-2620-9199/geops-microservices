package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.Offer;

import java.util.List;
import java.util.Optional;

public interface OfferRepositoryPort {
    Offer save(Offer offer);

    Optional<Offer> findById(Long id);

    List<Offer> findByCampaignId(Long campaignId);
}
