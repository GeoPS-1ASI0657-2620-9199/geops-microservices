package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.ports.BusinessProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BusinessProfileJpaAdapter implements BusinessProfileRepositoryPort {
    private final BusinessProfileJpaRepository repository;

    public BusinessProfileJpaAdapter(BusinessProfileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public BusinessProfile save(BusinessProfile businessProfile) {
        var saved = repository.save(BusinessProfilePersistenceMapper.toEntity(businessProfile));
        return BusinessProfilePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<BusinessProfile> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(BusinessProfilePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return repository.existsByUserId(userId);
    }

    @Override
    public boolean existsByRuc(Ruc ruc) {
        return repository.existsByRuc(ruc.number());
    }
}
