package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.GeoPoint;
import com.geopslabs.geops.identity.domain.models.Ruc;

import java.math.BigDecimal;

public final class BusinessProfilePersistenceMapper {
    private BusinessProfilePersistenceMapper() {
    }

    public static BusinessProfile toDomain(BusinessProfileJpaEntity entity) {
        var data = new BusinessProfile(entity.getBusinessName(), entity.getBusinessType(), new Ruc(entity.getRuc()),
                entity.getAddress(), locationOf(entity), entity.getOpeningHours());
        return new BusinessProfile(entity.getId(), entity.getUserId(), data, entity.getAccountStatus(),
                entity.getVerificationStatus());
    }

    public static BusinessProfileJpaEntity toEntity(BusinessProfile profile) {
        var entity = new BusinessProfileJpaEntity();
        entity.setId(profile.getId());
        entity.setUserId(profile.getUserId());
        entity.setBusinessName(profile.getBusinessName());
        entity.setBusinessType(profile.getBusinessType());
        entity.setRuc(profile.getRuc().number());
        entity.setAddress(profile.getAddress());
        entity.setOpeningHours(profile.getOpeningHours());
        entity.setAccountStatus(profile.getAccountStatus());
        entity.setVerificationStatus(profile.getVerificationStatus());
        setLocation(entity, profile.getLocation());
        return entity;
    }

    private static GeoPoint locationOf(BusinessProfileJpaEntity entity) {
        if (entity.getLatitude() == null || entity.getLongitude() == null) {
            return null;
        }
        return new GeoPoint(entity.getLatitude().doubleValue(), entity.getLongitude().doubleValue());
    }

    private static void setLocation(BusinessProfileJpaEntity entity, GeoPoint location) {
        if (location == null) {
            return;
        }
        entity.setLatitude(BigDecimal.valueOf(location.latitude()));
        entity.setLongitude(BigDecimal.valueOf(location.longitude()));
    }
}
