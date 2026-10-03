package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConsumerProfileJpaAdapter implements ConsumerProfileRepositoryPort {
    private final ConsumerProfileJpaRepository repository;

    public ConsumerProfileJpaAdapter(ConsumerProfileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ConsumerProfile save(ConsumerProfile consumerProfile) {
        var saved = repository.save(ConsumerProfilePersistenceMapper.toEntity(consumerProfile));
        return ConsumerProfilePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<ConsumerProfile> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(ConsumerProfilePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return repository.existsByUserId(userId);
    }
}
