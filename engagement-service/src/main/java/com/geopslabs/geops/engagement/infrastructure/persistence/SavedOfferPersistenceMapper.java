package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class SavedOfferPersistenceMapper {

    private SavedOfferPersistenceMapper() {
    }

    static SavedOffer toDomain(SavedOfferJpaEntity entity) {
        return new SavedOffer(entity.getId(), entity.getConsumerId(), entity.getOfferId(),
                toUtc(entity.getSavedAt()));
    }

    static SavedOfferJpaEntity toEntity(SavedOffer savedOffer, SavedOfferJpaEntity entity) {
        entity.setConsumerId(savedOffer.getConsumerId());
        entity.setOfferId(savedOffer.getOfferId());
        entity.setSavedAt(toInstant(savedOffer.getSavedAt()));
        return entity;
    }

    private static LocalDateTime toUtc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
