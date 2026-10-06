package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.MerchantStanding;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class MerchantStandingPersistenceMapper {

    private MerchantStandingPersistenceMapper() {
    }

    static MerchantStanding toDomain(MerchantStandingJpaEntity entity) {
        return new MerchantStanding(entity.getBusinessId(), entity.getBusinessName(), entity.isRucVerified(),
                entity.getOpenReports(), entity.getComplianceIndex(), entity.isVerifiedSeal(),
                LocalDateTime.ofInstant(entity.getUpdatedAt(), ZoneOffset.UTC));
    }

    static MerchantStandingJpaEntity toEntity(MerchantStanding standing) {
        var entity = new MerchantStandingJpaEntity();
        entity.setBusinessId(standing.getBusinessId());
        entity.setBusinessName(standing.getBusinessName());
        entity.setRucVerified(standing.isRucVerified());
        entity.setOpenReports(standing.getOpenReports());
        entity.setComplianceIndex(standing.getComplianceIndex());
        entity.setVerifiedSeal(standing.hasVerifiedSeal());
        entity.setUpdatedAt(standing.getUpdatedAt().toInstant(ZoneOffset.UTC));
        return entity;
    }
}
