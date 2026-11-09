package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.BusinessProfileView;
import com.geopslabs.geops.identity.application.usecases.GetBusinessProfileUseCase;
import com.geopslabs.geops.identity.domain.models.BusinessProfileAccessDeniedException;
import com.geopslabs.geops.identity.domain.models.BusinessProfileNotFoundException;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import com.geopslabs.geops.identity.domain.ports.RucVerificationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

/**
 * The owner's panel reads its profile with the verification status and the last attempt.
 */
@Transactional(readOnly = true)
public class BusinessProfileQueryService implements GetBusinessProfileUseCase {
    private final BusinessProfileRepositoryPort businessProfiles;
    private final RucVerificationRepositoryPort verifications;

    public BusinessProfileQueryService(BusinessProfileRepositoryPort businessProfiles,
                                       RucVerificationRepositoryPort verifications) {
        this.businessProfiles = businessProfiles;
        this.verifications = verifications;
    }

    @Override
    public BusinessProfileView get(Long businessId, Long requesterBusinessId) {
        if (!businessId.equals(requesterBusinessId)) {
            throw new BusinessProfileAccessDeniedException();
        }
        var profile = businessProfiles.findById(businessId).orElseThrow(BusinessProfileNotFoundException::new);
        return new BusinessProfileView(profile, verifications.findLatestByBusinessProfileId(businessId));
    }
}
