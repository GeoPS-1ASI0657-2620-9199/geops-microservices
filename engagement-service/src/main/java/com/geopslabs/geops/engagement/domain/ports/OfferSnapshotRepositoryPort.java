package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;

import java.util.Optional;

public interface OfferSnapshotRepositoryPort {
    Optional<OfferSnapshot> findById(Long offerId);

    java.util.List<OfferSnapshot> findAvailableByBusinessIds(java.util.Collection<Long> businessIds, java.time.LocalDate today);

    void upsert(OfferSnapshot offer);
}
