package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.commands.CreateOfferCommand;
import com.geopslabs.geops.catalog.infrastructure.web.CreateOfferResource;

public class CreateOfferCommandFromResourceAssembler {

    public static CreateOfferCommand toCommandFromResource(CreateOfferResource resource) {
        return new CreateOfferCommand(
            resource.campaignId(),
            resource.title(),
            resource.businessId(),
            resource.price(),
            resource.validTo(),
            resource.location(),
            resource.category(),
            resource.imageUrl()
        );
    }
}
