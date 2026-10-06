package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.commands.CreateCampaignCommand;
import com.geopslabs.geops.catalog.infrastructure.web.CreateCampaignResource;

public class CreateCampaignCommandFromResourceAssembler {
    public static CreateCampaignCommand toCommandFromResource(CreateCampaignResource resource) {
        return new CreateCampaignCommand(resource.businessId(), resource.name(), resource.description(), resource.startDate(),
                resource.endDate(), resource.estimatedBudget());
    }
}
