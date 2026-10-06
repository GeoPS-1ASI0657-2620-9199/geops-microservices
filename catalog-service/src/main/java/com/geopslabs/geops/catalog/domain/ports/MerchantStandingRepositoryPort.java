package com.geopslabs.geops.catalog.domain.ports;

import com.geopslabs.geops.catalog.domain.models.MerchantStanding;

import java.util.Optional;

public interface MerchantStandingRepositoryPort {
    MerchantStanding save(MerchantStanding standing);

    Optional<MerchantStanding> findByBusinessId(Long businessId);
}
