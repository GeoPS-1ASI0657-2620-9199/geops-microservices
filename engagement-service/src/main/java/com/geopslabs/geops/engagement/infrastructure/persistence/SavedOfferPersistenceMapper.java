package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;

final class SavedOfferPersistenceMapper {

    private SavedOfferPersistenceMapper() {
    }

    static SavedOffer toDomain(SavedOfferJpaEntity entity) {
        return new SavedOffer(entity.getId(), entity.getUser(), entity.getOffer(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static SavedOfferJpaEntity toEntity(SavedOffer savedOffer, SavedOfferJpaEntity entity) {
        entity.setUser(savedOffer.getUser());
        entity.setOffer(savedOffer.getOffer());
        return entity;
    }
}
