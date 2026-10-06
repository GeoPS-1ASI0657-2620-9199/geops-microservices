package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferCommandUseCase;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * SavedOfferCommandService
 *
 * Implementation of the SavedOfferCommandUseCase that handles all command operations
 * for saved offers. This service implements the business logic for
 * creating and managing saved offers following DDD principles
 *
 * @author GeOps Labs
 * @summary Implementation of saved offer command service operations
 * @since 1.0
 */
@Transactional
public class SavedOfferCommandService implements SavedOfferCommandUseCase {

    private final SavedOfferRepositoryPort savedOfferRepository;

    /**
     * Constructor for dependency injection
     *
     * @param savedOfferRepository The repository for saved offer data access
     */
    public SavedOfferCommandService(SavedOfferRepositoryPort savedOfferRepository) {
        this.savedOfferRepository = savedOfferRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SavedOffer> handle(SaveOfferCommand command) {
        try {
            // Check if saved offer already exists (prevent duplicates)
            boolean exists = savedOfferRepository.existsByConsumerIdAndOfferId(
                    command.consumerId(),
                    command.offerId()
            );

            if (exists) {
                System.err.println("SavedOffer already exists for consumerId: " +
                        command.consumerId() + " and offerId: " + command.offerId());
                return Optional.empty();
            }

            // Create new saved offer from command with consumer and offer ids
            var savedOffer = new SavedOffer(command);

            // Save the saved offer to the repository
            var savedSavedOffer = savedOfferRepository.save(savedOffer);

            return Optional.of(savedSavedOffer);

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error creating savedOffer: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handleDelete(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id cannot be null or negative");
        }

        try {
            // First check if saved offer exists
            if (!savedOfferRepository.existsById(id)) {
                return false;
            }

            // Delete the saved offer
            savedOfferRepository.deleteById(id);
            return true;

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error deleting savedOffer: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handleDelete(RemoveSavedOfferCommand command) {
        try {
            // First check if saved offer exists
            boolean exists = savedOfferRepository.existsByConsumerIdAndOfferId(
                    command.consumerId(),
                    command.offerId()
            );

            if (!exists) {
                System.err.println("SavedOffer not found for consumerId: " +
                        command.consumerId() + " and offerId: " + command.offerId());
                return false;
            }

            // Delete the saved offer by consumerId and offerId
            long deletedCount = savedOfferRepository.deleteByConsumerIdAndOfferId(
                    command.consumerId(),
                    command.offerId()
            );

            return deletedCount > 0;

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error deleting savedOffer by consumerId and offerId: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
