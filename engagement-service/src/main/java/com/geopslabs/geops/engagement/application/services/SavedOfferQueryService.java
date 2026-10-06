package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.SavedOfferQueryUseCase;
import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public class SavedOfferQueryService implements SavedOfferQueryUseCase {
    private final SavedOfferRepositoryPort savedOfferRepository;

    public SavedOfferQueryService(SavedOfferRepositoryPort savedOfferRepository) {
        this.savedOfferRepository = savedOfferRepository;
    }

    @Override
    public List<SavedOffer> handle(GetSavedOffersByConsumerQuery query) {
        return savedOfferRepository.findByConsumerId(query.consumerId());
    }
}
