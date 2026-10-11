package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.RucVerification;

import java.util.Optional;

public interface RucVerificationRepositoryPort {
    RucVerification save(RucVerification verification);

    Optional<RucVerification> findLatestByBusinessProfileId(Long businessProfileId);
}
