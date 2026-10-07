package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;
import com.geopslabs.geops.engagement.domain.ports.BusinessSnapshotRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
public class BusinessSnapshotJpaAdapter implements BusinessSnapshotRepositoryPort {
    private final BusinessSnapshotJpaRepository repository;
    private final Clock clock;

    public BusinessSnapshotJpaAdapter(BusinessSnapshotJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Optional<BusinessSnapshot> findById(Long businessId) {
        return repository.findById(businessId).map(SnapshotPersistenceMapper::toDomain);
    }

    @Override
    public void upsert(BusinessSnapshot business) {
        repository.upsert(business.businessId(), business.businessName(), clock.instant());
    }
}
