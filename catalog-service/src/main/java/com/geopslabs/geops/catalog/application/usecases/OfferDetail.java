package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Offer;

public record OfferDetail(Offer offer, String businessName, boolean verifiedSeal, boolean available) {
}
