package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.LoadPublicOffersCommand;
import com.geopslabs.geops.catalog.application.usecases.LoadPublicOffersUseCase;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.OfferCategory;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Saves the batch in one transaction: one offer with an invalid category rejects the whole batch. Each offer stays
 * PENDING without coordinates; the geocoding run gives them a location later (US07).
 */
@Transactional
public class PublicOfferCommandService implements LoadPublicOffersUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicOfferCommandService.class);

    private final OfferRepositoryPort offers;

    public PublicOfferCommandService(OfferRepositoryPort offers) {
        this.offers = offers;
    }

    @Override
    public List<Long> load(LoadPublicOffersCommand command) {
        var pending = command.offers().stream()
                .map(offer -> Offer.fromPublicSource(offer.title().strip(), offer.conditions().strip(),
                        Money.soles(offer.price()), offer.validTo(), OfferCategory.of(offer.category()).label(),
                        offer.address().strip(), offer.sourceName().strip(), offer.imageUrl()))
                .toList();
        var ids = pending.stream().map(offers::save).map(Offer::getId).toList();
        LOGGER.info("offers.public-source.loaded count={}", ids.size());
        return ids;
    }
}
