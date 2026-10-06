package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.Offer;

public final class OfferResourceFromEntityAssembler {

    private OfferResourceFromEntityAssembler() {
    }

    public static OfferResource toResourceFromEntity(Offer entity) {
        return new OfferResource(entity.getId(), entity.getCampaignId(), entity.getBusinessId(), entity.getTitle(),
                entity.getConditions(), entity.getPrice().amount(), entity.getValidTo(), entity.getCategory(),
                entity.getAddress(), entity.getImageUrl(), entity.getSource().name(), entity.getSourceName(),
                entity.getStatus().name());
    }
}
