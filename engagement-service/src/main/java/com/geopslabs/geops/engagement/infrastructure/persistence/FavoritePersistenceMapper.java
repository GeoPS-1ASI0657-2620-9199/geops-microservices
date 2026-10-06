package com.geopslabs.geops.engagement.infrastructure.persistence;

import com.geopslabs.geops.engagement.domain.models.Favorite;

final class FavoritePersistenceMapper {

    private FavoritePersistenceMapper() {
    }

    static Favorite toDomain(FavoriteJpaEntity entity) {
        return new Favorite(entity.getId(), entity.getUser(), entity.getOffer(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static FavoriteJpaEntity toEntity(Favorite favorite, FavoriteJpaEntity entity) {
        entity.setUser(favorite.getUser());
        entity.setOffer(favorite.getOffer());
        return entity;
    }
}
