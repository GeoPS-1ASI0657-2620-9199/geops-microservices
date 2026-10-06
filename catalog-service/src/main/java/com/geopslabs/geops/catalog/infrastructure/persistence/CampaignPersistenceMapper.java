package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.models.Campaign;

final class CampaignPersistenceMapper {

    private CampaignPersistenceMapper() {
    }

    static Campaign toDomain(CampaignJpaEntity entity) {
        return new Campaign(entity.getId(), entity.getUser(), entity.getName(), entity.getDescription(),
                entity.getStartDate(), entity.getEndDate(), entity.getStatus(), entity.getEstimatedBudget(),
                entity.getTotalImpressions(), entity.getTotalClicks(), entity.getCtr(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static CampaignJpaEntity toEntity(Campaign campaign) {
        var entity = new CampaignJpaEntity();
        entity.setId(campaign.getId());
        entity.setUser(campaign.getUser());
        entity.setName(campaign.getName());
        entity.setDescription(campaign.getDescription());
        entity.setStartDate(campaign.getStartDate());
        entity.setEndDate(campaign.getEndDate());
        entity.setStatus(campaign.getStatus());
        entity.setEstimatedBudget(campaign.getEstimatedBudget());
        entity.setTotalImpressions(campaign.getTotalImpressions());
        entity.setTotalClicks(campaign.getTotalClicks());
        entity.setCtr(campaign.getCTR());
        return entity;
    }
}
