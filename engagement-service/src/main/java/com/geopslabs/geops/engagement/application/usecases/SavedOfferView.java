package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.models.SavedOffer;

public record SavedOfferView(SavedOffer savedOffer, OfferSnapshot offer, String businessName, boolean expired) {
}
