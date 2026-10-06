package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MerchantStandingJpaAdapter implements MerchantStandingRepositoryPort {
    private final MerchantStandingJpaRepository repository;

    public MerchantStandingJpaAdapter(MerchantStandingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public MerchantStanding save(MerchantStanding standing) {
        var saved = repository.saveAndFlush(MerchantStandingPersistenceMapper.toEntity(standing));
        return MerchantStandingPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<MerchantStanding> findByBusinessId(Long businessId) {
        return repository.findById(businessId).map(MerchantStandingPersistenceMapper::toDomain);
    }
}
