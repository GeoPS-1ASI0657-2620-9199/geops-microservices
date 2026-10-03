package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;
import com.geopslabs.geops.identity.domain.ports.ConsumerProfileRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConsumerProfileJpaAdapter implements ConsumerProfileRepositoryPort {
    private final ConsumerProfileJpaRepository repository;
    private final UserJpaRepository userRepository;

    public ConsumerProfileJpaAdapter(ConsumerProfileJpaRepository repository, UserJpaRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Override
    public ConsumerProfile save(ConsumerProfile consumerProfile) {
        var entity = consumerProfile.getId() == null ? newEntity(consumerProfile)
                : repository.getReferenceById(consumerProfile.getId());
        ConsumerProfilePersistenceMapper.copyToEntity(consumerProfile, entity);
        return ConsumerProfilePersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<ConsumerProfile> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(ConsumerProfilePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return repository.existsByUserId(userId);
    }

    private ConsumerProfileJpaEntity newEntity(ConsumerProfile consumerProfile) {
        return new ConsumerProfileJpaEntity(userRepository.getReferenceById(consumerProfile.getUser().getId()));
    }
}
