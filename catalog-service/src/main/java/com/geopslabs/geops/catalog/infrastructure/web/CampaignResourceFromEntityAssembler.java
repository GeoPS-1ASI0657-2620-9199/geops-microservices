package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.infrastructure.web.CampaignResource;

public class CampaignResourceFromEntityAssembler {
    public static CampaignResource toResourceFromEntity(Campaign entity)
    {
        return new CampaignResource(entity.getId(), entity.getBusinessId(), entity.getName(), entity.getDescription(),entity.getStartDate(),
                entity.getEndDate(), entity.getStatus().toString(), entity.getEstimatedBudget());
    }
}
