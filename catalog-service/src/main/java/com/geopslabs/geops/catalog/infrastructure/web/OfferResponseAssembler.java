package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.Offer;

public final class OfferResponseAssembler {

    private OfferResponseAssembler() {
    }

    public static OfferResponse toResponse(Offer offer) {
        return new OfferResponse(offer.getId(), offer.getCampaignId(), offer.getBusinessId(), offer.getTitle(),
                offer.getConditions(), offer.getPrice().amount(), offer.getValidTo(), offer.getCategory(),
                offer.getAddress(), offer.getImageUrl(), offer.getSource().name(), offer.getSourceName(),
                offer.getStatus().name());
    }
}
