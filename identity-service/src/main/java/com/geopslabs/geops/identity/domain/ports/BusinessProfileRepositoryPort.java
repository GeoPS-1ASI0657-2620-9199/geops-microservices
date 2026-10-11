package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;

import java.util.List;
import java.util.Optional;

public interface BusinessProfileRepositoryPort {
    BusinessProfile save(BusinessProfile businessProfile);

    Optional<BusinessProfile> findById(Long id);

    /**
     * Reads the profile and locks it until the caller's transaction ends, so two writers of the same profile apply
     * their changes one after the other. Must be called inside a transaction.
     */
    Optional<BusinessProfile> findByIdForUpdate(Long id);

    Optional<BusinessProfile> findByUserId(Long userId);

    List<Long> findIdsByVerificationStatus(VerificationStatus status);

    boolean existsByUserId(Long userId);

    boolean existsByRuc(Ruc ruc);
}
