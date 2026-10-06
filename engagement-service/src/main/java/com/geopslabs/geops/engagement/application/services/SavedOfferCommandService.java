package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.application.usecases.SavedOfferCommandUseCase;
import com.geopslabs.geops.engagement.domain.ports.SavedOfferRepositoryPort;
import com.geopslabs.geops.backend.identity.infrastructure.persistence.jpa.UserRepository;
import com.geopslabs.geops.backend.notifications.application.internal.outboundservices.NotificationFactoryService;
import com.geopslabs.geops.backend.offers.infrastructure.persistence.jpa.OfferRepository;
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
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final NotificationFactoryService notificationFactory;

    /**
     * Constructor for dependency injection
     *
     * @param savedOfferRepository The repository for saved offer data access
     * @param userRepository The repository for user data access
     * @param offerRepository The repository for offer data access
     * @param notificationFactory Service to create notifications
     */
    public SavedOfferCommandService(
        SavedOfferRepositoryPort savedOfferRepository,
        UserRepository userRepository,
        OfferRepository offerRepository,
        NotificationFactoryService notificationFactory
    ) {
        this.savedOfferRepository = savedOfferRepository;
        this.userRepository = userRepository;
        this.offerRepository = offerRepository;
        this.notificationFactory = notificationFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<SavedOffer> handle(SaveOfferCommand command) {
        try {
            // Check if saved offer already exists (prevent duplicates)
            boolean exists = savedOfferRepository.existsByUserIdAndOfferId(
                    command.userId(),
                    command.offerId()
            );

            if (exists) {
                System.err.println("SavedOffer already exists for userId: " +
                        command.userId() + " and offerId: " + command.offerId());
                return Optional.empty();
            }

            // Fetch user entity
            var userOptional = userRepository.findById(command.userId());
            
            if (userOptional.isEmpty()) {
                System.err.println("User not found: " + command.userId());
                return Optional.empty();
            }

            // Fetch offer entity
            var offerOptional = offerRepository.findById(command.offerId());
            
            if (offerOptional.isEmpty()) {
                System.err.println("Offer not found: " + command.offerId());
                return Optional.empty();
            }

            // Create new saved offer from command with user and offer entities
            var savedOffer = new SavedOffer(command, userOptional.get(), offerOptional.get());

            // Save the saved offer to the repository
            var savedSavedOffer = savedOfferRepository.save(savedOffer);

            // Create notification for saved offer added
            notificationFactory.createFavoriteAddedNotification(
                command.userId(),
                command.offerId().toString(),
                "Oferta"
            );

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
            boolean exists = savedOfferRepository.existsByUserIdAndOfferId(
                    command.userId(),
                    command.offerId()
            );

            if (!exists) {
                System.err.println("SavedOffer not found for userId: " +
                        command.userId() + " and offerId: " + command.offerId());
                return false;
            }

            // Delete the saved offer by userId and offerId
            long deletedCount = savedOfferRepository.deleteByUserIdAndOfferId(
                    command.userId(),
                    command.offerId()
            );

            return deletedCount > 0;

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error deleting savedOffer by userId and offerId: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
