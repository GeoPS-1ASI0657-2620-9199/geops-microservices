package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;

import java.util.Optional;

public interface BusinessSnapshotRepositoryPort {
    Optional<BusinessSnapshot> findById(Long businessId);

    void upsert(BusinessSnapshot business);
}
