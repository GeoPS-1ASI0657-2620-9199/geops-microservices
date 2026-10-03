package com.geopslabs.geops.identity.infrastructure.persistence;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;

import java.math.BigDecimal;

public final class BusinessProfilePersistenceMapper {
    private BusinessProfilePersistenceMapper() {
    }

    public static BusinessProfile toDomain(BusinessProfileJpaEntity entity) {
        var data = new BusinessProfile(entity.getUserId(), entity.getBusinessName(), entity.getBusinessType(),
                entity.getRuc(), entity.getAddress(), entity.getOpeningHours());
        return new BusinessProfile(entity.getId(), data, toDouble(entity.getLatitude()),
                toDouble(entity.getLongitude()), entity.getAccountStatus(), entity.getVerificationStatus());
    }

    public static BusinessProfileJpaEntity toEntity(BusinessProfile profile) {
        var entity = new BusinessProfileJpaEntity();
        entity.setId(profile.getId());
        entity.setUserId(profile.getUserId());
        entity.setBusinessName(profile.getBusinessName());
        entity.setBusinessType(profile.getBusinessType());
        entity.setRuc(profile.getRuc());
        entity.setAddress(profile.getAddress());
        entity.setLatitude(toBigDecimal(profile.getLatitude()));
        entity.setLongitude(toBigDecimal(profile.getLongitude()));
        entity.setOpeningHours(profile.getOpeningHours());
        entity.setAccountStatus(profile.getAccountStatus());
        entity.setVerificationStatus(profile.getVerificationStatus());
        return entity;
    }

    private static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
