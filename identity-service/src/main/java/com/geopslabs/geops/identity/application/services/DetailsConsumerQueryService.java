package com.geopslabs.geops.identity.application.services;

import com.geopslabs.geops.identity.application.usecases.DetailsConsumerQueryUseCase;
import com.geopslabs.geops.identity.application.usecases.GetDetailsConsumerByUserIdQuery;
import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional(readOnly = true)
public class DetailsConsumerQueryService implements DetailsConsumerQueryUseCase {
    private final ConsumerProfileRepositoryPort consumerProfileRepository;

    public DetailsConsumerQueryService(ConsumerProfileRepositoryPort consumerProfileRepository) {
        this.consumerProfileRepository = consumerProfileRepository;
    }

    @Override
    public Optional<ConsumerProfile> handle(GetDetailsConsumerByUserIdQuery query) {
        return consumerProfileRepository.findByUserId(query.userId());
    }
}
