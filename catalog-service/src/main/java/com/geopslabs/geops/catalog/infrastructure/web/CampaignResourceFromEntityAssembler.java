package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.Campaign;

public final class CampaignResourceFromEntityAssembler {

    private CampaignResourceFromEntityAssembler() {
    }

    public static CampaignResource toResourceFromEntity(Campaign entity) {
        return new CampaignResource(entity.getId(), entity.getBusinessId(), entity.getName(), entity.getDescription(),
                entity.getStartDate(), entity.getEndDate(), entity.getStatus().name(), entity.getEstimatedBudget());
    }
}
