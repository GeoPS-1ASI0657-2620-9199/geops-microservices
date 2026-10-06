package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.CampaignZone;
import com.geopslabs.geops.catalog.domain.models.DateRange;
import com.geopslabs.geops.catalog.domain.models.Money;

final class CampaignPersistenceMapper {

    private CampaignPersistenceMapper() {
    }

    static Campaign toDomain(CampaignJpaEntity entity) {
        var period = new DateRange(entity.getStartDate(), entity.getEndDate());
        var zone = new CampaignZone(entity.getZoneType(), entity.getZoneRadiusMeters(), entity.getZoneDistrict());
        return new Campaign(entity.getId(), entity.getBusinessId(), entity.getName(), entity.getDescription(),
                period, zone, entity.getStatus(), Money.soles(entity.getEstimatedBudget()));
    }

    static CampaignJpaEntity toEntity(Campaign campaign) {
        var entity = new CampaignJpaEntity();
        entity.setId(campaign.getId());
        entity.setBusinessId(campaign.getBusinessId());
        entity.setName(campaign.getName());
        entity.setDescription(campaign.getDescription());
        entity.setStartDate(campaign.getPeriod().start());
        entity.setEndDate(campaign.getPeriod().end());
        entity.setStatus(campaign.getStatus());
        entity.setZoneType(campaign.getZone().type());
        entity.setZoneRadiusMeters(campaign.getZone().radiusMeters());
        entity.setZoneDistrict(campaign.getZone().district());
        entity.setEstimatedBudget(campaign.getEstimatedBudget().amount());
        return entity;
    }
}
