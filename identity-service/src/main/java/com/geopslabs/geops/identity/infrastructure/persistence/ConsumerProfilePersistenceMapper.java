package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;

public final class ConsumerProfilePersistenceMapper {
    private ConsumerProfilePersistenceMapper() {
    }

    public static ConsumerProfile toDomain(ConsumerProfileJpaEntity entity) {
        var data = new ConsumerProfile(UserPersistenceMapper.toDomain(entity.getUser()),
                entity.getCategoriasFavoritas(), entity.getPermisoUbicacion(),
                entity.getDireccionCasa(), entity.getDireccionTrabajo(), entity.getDireccionUniversidad());
        return new ConsumerProfile(entity.getId(), data, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static void copyToEntity(ConsumerProfile profile, ConsumerProfileJpaEntity entity) {
        entity.update(profile.getCategoriasFavoritas(), profile.getPermisoUbicacion(), profile.getDireccionCasa(),
                profile.getDireccionTrabajo(), profile.getDireccionUniversidad());
    }
}
