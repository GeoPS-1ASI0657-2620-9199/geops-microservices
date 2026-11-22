package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;

import java.util.Optional;

/** business_baselines: one row per business, replaced when it declares again. */
public interface BusinessBaselineRepositoryPort {
    BusinessBaseline save(BusinessBaseline baseline);

    Optional<BusinessBaseline> findByBusinessId(Long businessId);
}
