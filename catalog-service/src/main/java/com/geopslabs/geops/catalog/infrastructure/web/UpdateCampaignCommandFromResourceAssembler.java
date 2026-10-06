package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.commands.UpdateCampaignCommand;
import com.geopslabs.geops.catalog.infrastructure.web.UpdateCampaignResource;

public class UpdateCampaignCommandFromResourceAssembler {
    public static UpdateCampaignCommand toCommandFromResource(Long id, UpdateCampaignResource resource) {
        return new UpdateCampaignCommand(id, resource.name(), resource.description(), resource.startDate(),
                resource.endDate(), resource.status(), resource.estimatedBudget());
    }
}
