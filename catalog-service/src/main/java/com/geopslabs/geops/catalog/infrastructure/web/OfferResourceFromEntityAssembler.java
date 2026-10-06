package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.infrastructure.web.OfferResource;

public class OfferResourceFromEntityAssembler {

    public static OfferResource toResourceFromEntity(Offer entity) {
        return new OfferResource(
            entity.getId(),
            entity.getCampaign().getId(),
            entity.getTitle(),
            entity.getBusinessId(),
            entity.getPrice(),
            entity.getValidTo(),
            entity.getLocation(),
            entity.getCategory(),
            entity.getImageUrl()
        );
    }
}
