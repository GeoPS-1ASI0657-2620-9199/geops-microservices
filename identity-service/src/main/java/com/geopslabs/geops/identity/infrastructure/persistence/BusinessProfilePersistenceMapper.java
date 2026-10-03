package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;

public final class BusinessProfilePersistenceMapper {
    private BusinessProfilePersistenceMapper() {
    }

    public static BusinessProfile toDomain(BusinessProfileJpaEntity entity) {
        var data = new BusinessProfile(entity.getUser().getId(),
                entity.getBusinessName(), entity.getBusinessType(), entity.getTaxId(), entity.getWebsite(),
                entity.getDescription(), entity.getAddress(), entity.getHorarioAtencion());
        return new BusinessProfile(entity.getId(), data, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static void copyToEntity(BusinessProfile profile, BusinessProfileJpaEntity entity) {
        entity.update(profile.getBusinessName(), profile.getBusinessType(), profile.getTaxId(),
                profile.getWebsite(), profile.getDescription(), profile.getAddress(),
                profile.getHorarioAtencion());
    }
}
