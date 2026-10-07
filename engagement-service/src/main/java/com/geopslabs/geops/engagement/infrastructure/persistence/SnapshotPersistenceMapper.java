package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.BusinessSnapshot;
import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class SnapshotPersistenceMapper {

    private SnapshotPersistenceMapper() {
    }

    static OfferSnapshot toDomain(OfferSnapshotJpaEntity entity) {
        return new OfferSnapshot(entity.getOfferId(), entity.getBusinessId(), entity.getTitle(), entity.getValidTo(),
                entity.getStatus());
    }

    static BusinessSnapshot toDomain(BusinessSnapshotJpaEntity entity) {
        return new BusinessSnapshot(entity.getBusinessId(), entity.getBusinessName());
    }

    static RedeemedReservation toDomain(RedeemedReservationJpaEntity entity) {
        return new RedeemedReservation(entity.getReservationId(), entity.getConsumerId(), entity.getBusinessId(),
                LocalDateTime.ofInstant(entity.getRedeemedAt(), ZoneOffset.UTC));
    }

    static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC);
    }
}
