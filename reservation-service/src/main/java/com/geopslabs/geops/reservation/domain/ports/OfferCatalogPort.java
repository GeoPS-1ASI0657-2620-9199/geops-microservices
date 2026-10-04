package com.geopslabs.geops.reservation.domain.ports;

import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;

public interface OfferCatalogPort {
    OfferSnapshot findValidOffer(Long offerId);
}
