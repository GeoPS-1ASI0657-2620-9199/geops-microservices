package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.GetOfferByIdUseCase;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.exceptions.OfferNotFoundException;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;

public class OfferQueryService implements GetOfferByIdUseCase {
    private final OfferRepositoryPort offerRepository;

    public OfferQueryService(OfferRepositoryPort offerRepository) {
        this.offerRepository = offerRepository;
    }

    @Override
    public Offer getById(GetOfferByIdQuery query) {
        return offerRepository.findById(query.offerId())
                .orElseThrow(() -> new OfferNotFoundException(query.offerId()));
    }
}
