package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;

import java.util.Optional;

public interface BusinessProfileRepositoryPort {
    BusinessProfile save(BusinessProfile businessProfile);

    Optional<BusinessProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
