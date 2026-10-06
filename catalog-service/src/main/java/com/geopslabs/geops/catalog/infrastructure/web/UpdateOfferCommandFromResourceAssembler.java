package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.commands.UpdateOfferCommand;
import com.geopslabs.geops.catalog.infrastructure.web.UpdateOfferResource;

public class UpdateOfferCommandFromResourceAssembler {

    public static UpdateOfferCommand toCommandFromResource(Long id, UpdateOfferResource resource) {
        return new UpdateOfferCommand(
            id,
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

