package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;
import com.geopslabs.geops.engagement.domain.ports.RedeemedReservationRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RedeemedReservationJpaAdapter implements RedeemedReservationRepositoryPort {
    private static final int INSERTED = 1;

    private final RedeemedReservationJpaRepository repository;

    public RedeemedReservationJpaAdapter(RedeemedReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RedeemedReservation> findUnreviewed(Long consumerId, Long businessId) {
        return repository.findOldestUnreviewed(consumerId, businessId).map(SnapshotPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsFor(Long consumerId, Long businessId) {
        return repository.existsByConsumerIdAndBusinessId(consumerId, businessId);
    }

    @Override
    public boolean saveIfAbsent(RedeemedReservation redemption) {
        var inserted = repository.insertIfAbsent(redemption.reservationId(), redemption.consumerId(),
                redemption.businessId(), SnapshotPersistenceMapper.toInstant(redemption.redeemedAt()));
        return inserted == INSERTED;
    }
}
