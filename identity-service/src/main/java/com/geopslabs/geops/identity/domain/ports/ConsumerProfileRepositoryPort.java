package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;

import java.util.Optional;

public interface ConsumerProfileRepositoryPort {
    ConsumerProfile save(ConsumerProfile consumerProfile);

    Optional<ConsumerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
