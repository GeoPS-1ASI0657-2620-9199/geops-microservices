package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;

import java.util.Optional;

public final class OfferResponseAssembler {

    private OfferResponseAssembler() {
    }

    public static OfferResponse toResponse(Offer offer) {
        return new OfferResponse(offer.getId(), offer.getCampaignId(), offer.getBusinessId(), offer.getTitle(),
                offer.getConditions(), offer.getPrice().amount(),
                Optional.ofNullable(offer.getRegularPrice()).map(Money::amount).orElse(null), offer.getValidTo(), offer.getCategory(),
                offer.getAddress(), offer.getImageUrl(), offer.getSource().name(), offer.getSourceName(),
                offer.getStatus().name());
    }
}
