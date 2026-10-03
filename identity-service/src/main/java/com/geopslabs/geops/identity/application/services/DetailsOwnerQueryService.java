package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.DetailsOwnerQueryUseCase;
import com.geopslabs.geops.identity.application.usecases.GetDetailsOwnerByUserIdQuery;
import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public class DetailsOwnerQueryService implements DetailsOwnerQueryUseCase {
    private final BusinessProfileRepositoryPort businessProfileRepository;

    public DetailsOwnerQueryService(BusinessProfileRepositoryPort businessProfileRepository) {
        this.businessProfileRepository = businessProfileRepository;
    }

    @Override
    public Optional<BusinessProfile> handle(GetDetailsOwnerByUserIdQuery query) {
        return businessProfileRepository.findByUserId(query.userId());
    }
}
