package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;

public final class ConsumerProfilePersistenceMapper {
    private ConsumerProfilePersistenceMapper() {
    }

    public static ConsumerProfile toDomain(ConsumerProfileJpaEntity entity) {
        var data = new ConsumerProfile(entity.getUserId(), entity.getLocationPermission(),
                entity.getSearchRadiusMinutes(), entity.getDefaultDistrict());
        return new ConsumerProfile(entity.getId(), data);
    }

    public static ConsumerProfileJpaEntity toEntity(ConsumerProfile profile) {
        var entity = new ConsumerProfileJpaEntity();
        entity.setId(profile.getId());
        entity.setUserId(profile.getUserId());
        entity.setLocationPermission(profile.isLocationPermission());
        entity.setSearchRadiusMinutes(profile.getSearchRadiusMinutes());
        entity.setDefaultDistrict(profile.getDefaultDistrict());
        return entity;
    }
}
