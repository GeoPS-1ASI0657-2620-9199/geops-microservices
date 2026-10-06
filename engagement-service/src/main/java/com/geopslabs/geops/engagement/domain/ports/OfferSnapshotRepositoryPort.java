package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;

import java.util.Optional;

public interface OfferSnapshotRepositoryPort {
    Optional<OfferSnapshot> findById(Long offerId);
}
