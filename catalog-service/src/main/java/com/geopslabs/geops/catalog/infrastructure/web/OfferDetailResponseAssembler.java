package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.OfferDetail;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;

import java.util.Optional;

public final class OfferDetailResponseAssembler {

    private OfferDetailResponseAssembler() {
    }

    public static OfferDetailResponse toResponse(OfferDetail detail) {
        var offer = detail.offer();
        var location = Optional.ofNullable(offer.getLocation());
        return new OfferDetailResponse(offer.getId(), offer.getTitle(), offer.getConditions(),
                offer.getPrice().amount(), offer.getValidTo(), offer.getCategory(), offer.getAddress(),
                location.map(GeoPoint::latitude).orElse(null), location.map(GeoPoint::longitude).orElse(null),
                offer.getImageUrl(), offer.getSource().name(), offer.getBusinessId(), detail.businessName(),
                detail.verifiedSeal(), detail.available());
    }
}
