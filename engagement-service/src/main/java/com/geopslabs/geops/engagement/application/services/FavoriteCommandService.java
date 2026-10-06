package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.Favorite;
import com.geopslabs.geops.engagement.domain.models.commands.CreateFavoriteCommand;
import com.geopslabs.geops.engagement.domain.models.commands.DeleteFavoriteCommand;
import com.geopslabs.geops.engagement.application.usecases.FavoriteCommandUseCase;
import com.geopslabs.geops.engagement.domain.ports.FavoriteRepositoryPort;
import com.geopslabs.geops.backend.identity.infrastructure.persistence.jpa.UserRepository;
import com.geopslabs.geops.backend.notifications.application.internal.outboundservices.NotificationFactoryService;
import com.geopslabs.geops.backend.offers.infrastructure.persistence.jpa.OfferRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * FavoriteCommandService
 *
 * Implementation of the FavoriteCommandUseCase that handles all command operations
 * for favorites. This service implements the business logic for
 * creating and managing favorites following DDD principles
 *
 * @author GeOps Labs
 * @summary Implementation of favorite command service operations
 * @since 1.0
 */
@Transactional
public class FavoriteCommandService implements FavoriteCommandUseCase {

    private final FavoriteRepositoryPort favoriteRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final NotificationFactoryService notificationFactory;

    /**
     * Constructor for dependency injection
     *
     * @param favoriteRepository The repository for favorite data access
     * @param userRepository The repository for user data access
     * @param offerRepository The repository for offer data access
     * @param notificationFactory Service to create notifications
     */
    public FavoriteCommandService(
        FavoriteRepositoryPort favoriteRepository,
        UserRepository userRepository,
        OfferRepository offerRepository,
        NotificationFactoryService notificationFactory
    ) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.offerRepository = offerRepository;
        this.notificationFactory = notificationFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Favorite> handle(CreateFavoriteCommand command) {
        try {
            // Check if favorite already exists (prevent duplicates)
            boolean exists = favoriteRepository.existsByUserIdAndOfferId(
                    command.userId(),
                    command.offerId()
            );

            if (exists) {
                System.err.println("Favorite already exists for userId: " +
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

            // Create new favorite from command with user and offer entities
            var favorite = new Favorite(command, userOptional.get(), offerOptional.get());

            // Save the favorite to the repository
            var savedFavorite = favoriteRepository.save(favorite);

            // Create notification for favorite added
            notificationFactory.createFavoriteAddedNotification(
                command.userId(),
                command.offerId().toString(),
                "Oferta"
            );

            return Optional.of(savedFavorite);

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error creating favorite: " + e.getMessage());
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
            // First check if favorite exists
            if (!favoriteRepository.existsById(id)) {
                return false;
            }

            // Delete the favorite
            favoriteRepository.deleteById(id);
            return true;

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error deleting favorite: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handleDelete(DeleteFavoriteCommand command) {
        try {
            // First check if favorite exists
            boolean exists = favoriteRepository.existsByUserIdAndOfferId(
                    command.userId(),
                    command.offerId()
            );

            if (!exists) {
                System.err.println("Favorite not found for userId: " +
                        command.userId() + " and offerId: " + command.offerId());
                return false;
            }

            // Delete the favorite by userId and offerId
            long deletedCount = favoriteRepository.deleteByUserIdAndOfferId(
                    command.userId(),
                    command.offerId()
            );

            return deletedCount > 0;

        } catch (Exception e) {
            // Log the error with full stacktrace
            System.err.println("Error deleting favorite by userId and offerId: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
