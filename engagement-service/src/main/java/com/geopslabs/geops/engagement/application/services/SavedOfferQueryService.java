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

/**
 * SavedOfferQueryService
 *
 * Implementation of the SavedOfferQueryUseCase that handles all query operations
 * for saved offers. This service implements the business logic for
 * retrieving and searching saved offers following DDD principles.
 *
 * @summary Implementation of saved offer query service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Transactional(readOnly = true)
public class SavedOfferQueryService implements SavedOfferQueryUseCase {

    private final SavedOfferRepositoryPort savedOfferRepository;

    /**
     * Constructor for dependency injection
     *
     * @param savedOfferRepository The repository for saved offer data access
     */
    public SavedOfferQueryService(SavedOfferRepositoryPort savedOfferRepository) {
        this.savedOfferRepository = savedOfferRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SavedOffer> handle(GetSavedOffersByConsumerQuery query) {
        try {
            return savedOfferRepository.findByConsumerId(query.consumerId());
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving savedOffers by consumer ID: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SavedOffer> handle(GetSavedOfferByConsumerIdAndOfferIdQuery query) {
        try {
            return savedOfferRepository.findByConsumerIdAndOfferId(
                query.consumerId(),
                query.offerId()
            );
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving savedOffer by consumerId and offerId: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SavedOffer> handle(GetSavedOfferByIdQuery query) {
        try {
            return savedOfferRepository.findById(query.id());
        } catch (Exception e) {
            // Log the error
            System.err.println("Error retrieving savedOffer by ID: " + e.getMessage());
            return Optional.empty();
        }
    }
}


