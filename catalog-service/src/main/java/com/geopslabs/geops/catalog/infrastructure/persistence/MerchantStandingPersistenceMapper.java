package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.MerchantStanding;


final class MerchantStandingPersistenceMapper {

    private MerchantStandingPersistenceMapper() {
    }

    static MerchantStanding toDomain(MerchantStandingJpaEntity entity) {
        return new MerchantStanding(entity.getBusinessId(), entity.getBusinessName(), entity.isRucVerified(),
                entity.getOpenReports(), entity.getComplianceIndex(), entity.isVerifiedSeal(),
                entity.getUpdatedAt(), entity.getRedeemedReservations(), entity.getConsumerFavorReports());
    }

    static MerchantStandingJpaEntity toEntity(MerchantStanding standing) {
        var entity = new MerchantStandingJpaEntity();
        entity.setBusinessId(standing.getBusinessId());
        entity.setBusinessName(standing.getBusinessName());
        entity.setRucVerified(standing.isRucVerified());
        entity.setOpenReports(standing.getOpenReports());
        entity.setComplianceIndex(standing.getComplianceIndex());
        entity.setVerifiedSeal(standing.hasVerifiedSeal());
        entity.setUpdatedAt(standing.getUpdatedAt());
        entity.setRedeemedReservations(standing.getRedeemedReservations());
        entity.setConsumerFavorReports(standing.getConsumerFavorReports());
        return entity;
    }
}
