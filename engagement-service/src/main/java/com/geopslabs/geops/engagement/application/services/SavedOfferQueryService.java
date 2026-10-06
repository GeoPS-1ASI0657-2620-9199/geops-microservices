package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOfferByIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOfferByConsumerIdAndOfferIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferQueryUseCase;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public class SavedOfferQueryService implements SavedOfferQueryUseCase {
    private final SavedOfferRepositoryPort savedOfferRepository;

    public SavedOfferQueryService(SavedOfferRepositoryPort savedOfferRepository) {
        this.savedOfferRepository = savedOfferRepository;
    }

    @Override
    public List<SavedOffer> handle(GetSavedOffersByConsumerQuery query) {
        try {
            return savedOfferRepository.findByConsumerId(query.consumerId());
        } catch (Exception e) {
            System.err.println("Error retrieving savedOffers by consumer ID: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<SavedOffer> handle(GetSavedOfferByConsumerIdAndOfferIdQuery query) {
        try {
            return savedOfferRepository.findByConsumerIdAndOfferId(
                query.consumerId(),
                query.offerId()
            );
        } catch (Exception e) {
            System.err.println("Error retrieving savedOffer by consumerId and offerId: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<SavedOffer> handle(GetSavedOfferByIdQuery query) {
        try {
            return savedOfferRepository.findById(query.id());
        } catch (Exception e) {
            System.err.println("Error retrieving savedOffer by ID: " + e.getMessage());
            return Optional.empty();
        }
    }
}

