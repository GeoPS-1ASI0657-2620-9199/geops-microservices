package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class SavedOfferPersistenceMapper {

    private SavedOfferPersistenceMapper() {
    }

    static SavedOffer toDomain(SavedOfferJpaEntity entity) {
        return new SavedOffer(entity.getId(), entity.getConsumerId(), entity.getOfferId(),
                LocalDateTime.ofInstant(entity.getSavedAt(), ZoneOffset.UTC));
    }

    static SavedOfferJpaEntity toEntity(SavedOffer savedOffer) {
        var entity = new SavedOfferJpaEntity();
        entity.setId(savedOffer.getId());
        entity.setConsumerId(savedOffer.getConsumerId());
        entity.setOfferId(savedOffer.getOfferId());
        entity.setSavedAt(savedOffer.getSavedAt().toInstant(ZoneOffset.UTC));
        return entity;
    }
}
