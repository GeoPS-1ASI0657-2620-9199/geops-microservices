package com.geopslabs.geops.reservation.acceptance;

import com.geopslabs.geops.reservation.domain.models.OfferSnapshot;
import com.geopslabs.geops.reservation.domain.models.exceptions.CatalogUnavailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotAvailableException;
import com.geopslabs.geops.reservation.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.reservation.domain.ports.OfferCatalogPort;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FakeOfferCatalog implements OfferCatalogPort {
    private final Map<Long, OfferSnapshot> offers = new ConcurrentHashMap<>();
    private final Set<Long> unavailableOffers = ConcurrentHashMap.newKeySet();
    private volatile boolean down;

    public void reset() {
        offers.clear();
        unavailableOffers.clear();
        down = false;
    }

    public void publish(OfferSnapshot offer) {
        offers.put(offer.offerId(), offer);
    }

    public void markUnavailable(Long offerId) {
        unavailableOffers.add(offerId);
    }

    public void goDown() {
        down = true;
    }

    @Override
    public OfferSnapshot findValidOffer(Long offerId) {
        if (down) {
            throw new CatalogUnavailableException();
        }
        if (unavailableOffers.contains(offerId)) {
            throw new OfferNotAvailableException(offerId);
        }
        return Optional.ofNullable(offers.get(offerId)).orElseThrow(() -> new OfferNotFoundException(offerId));
    }
}
