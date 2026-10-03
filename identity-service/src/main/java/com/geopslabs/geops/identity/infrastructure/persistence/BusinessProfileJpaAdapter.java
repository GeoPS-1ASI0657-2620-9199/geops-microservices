package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BusinessProfileJpaAdapter implements BusinessProfileRepositoryPort {
    private final BusinessProfileJpaRepository repository;
    private final UserJpaRepository userRepository;

    public BusinessProfileJpaAdapter(BusinessProfileJpaRepository repository, UserJpaRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Override
    public BusinessProfile save(BusinessProfile businessProfile) {
        var entity = businessProfile.getId() == null ? newEntity(businessProfile)
                : repository.getReferenceById(businessProfile.getId());
        BusinessProfilePersistenceMapper.copyToEntity(businessProfile, entity);
        return BusinessProfilePersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<BusinessProfile> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(BusinessProfilePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return repository.existsByUserId(userId);
    }

    private BusinessProfileJpaEntity newEntity(BusinessProfile businessProfile) {
        return new BusinessProfileJpaEntity(userRepository.getReferenceById(businessProfile.getUserId()));
    }
}
