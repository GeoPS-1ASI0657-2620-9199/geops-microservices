package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.Campaign;

public final class CampaignResourceFromEntityAssembler {

    private CampaignResourceFromEntityAssembler() {
    }

    public static CampaignResource toResourceFromEntity(Campaign entity) {
        var zone = entity.getZone();
        return new CampaignResource(entity.getId(), entity.getBusinessId(), entity.getName(), entity.getDescription(),
                entity.getPeriod().start(), entity.getPeriod().end(), zone.type().name(), zone.radiusMeters(),
                zone.district(), entity.getStatus().name(), entity.getEstimatedBudget().amount());
    }
}
