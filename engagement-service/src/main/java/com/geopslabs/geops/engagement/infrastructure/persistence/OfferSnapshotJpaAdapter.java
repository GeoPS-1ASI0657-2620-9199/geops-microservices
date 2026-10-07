package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.ports.OfferSnapshotRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
public class OfferSnapshotJpaAdapter implements OfferSnapshotRepositoryPort {
    private final OfferSnapshotJpaRepository repository;
    private final Clock clock;

    public OfferSnapshotJpaAdapter(OfferSnapshotJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Optional<OfferSnapshot> findById(Long offerId) {
        return repository.findById(offerId).map(SnapshotPersistenceMapper::toDomain);
    }

    @Override
    public void upsert(OfferSnapshot offer) {
        repository.upsert(offer.offerId(), offer.businessId(), offer.title(), offer.validTo(), offer.status(),
                clock.instant());
    }
}
