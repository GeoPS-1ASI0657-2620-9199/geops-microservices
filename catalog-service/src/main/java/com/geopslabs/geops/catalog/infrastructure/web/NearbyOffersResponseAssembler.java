package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.NearbyOffersPage;
import com.geopslabs.geops.catalog.domain.models.RankedOffer;

final class NearbyOffersResponseAssembler {

    private NearbyOffersResponseAssembler() {
    }

    static NearbyOffersResponse toResponse(NearbyOffersPage page) {
        return new NearbyOffersResponse(page.content().stream().map(NearbyOffersResponseAssembler::toResponse).toList(),
                page.page(), page.totalElements(), page.totalPages());
    }

    private static NearbyOfferResponse toResponse(RankedOffer ranked) {
        var offer = ranked.offer();
        return new NearbyOfferResponse(offer.offerId(), offer.title(), offer.businessId(), offer.businessName(),
                offer.verifiedSeal(), Math.round(ranked.distanceMeters()), ranked.walkMinutes(), offer.category(),
                offer.address(), offer.imageUrl(), offer.location().latitude(), offer.location().longitude(),
                offer.price(), offer.validTo());
    }
}
