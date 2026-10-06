package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.Favorite;
import com.geopslabs.geops.engagement.domain.models.commands.CreateFavoriteCommand;
import com.geopslabs.geops.engagement.domain.models.commands.DeleteFavoriteCommand;

import java.util.Optional;

/**
 * FavoriteCommandService
 *
 * Domain service interface that defines command operations for managing favorites.
 * This service handles all write operations (Create, Delete) following the
 * Command Query Responsibility Segregation (CQRS) pattern
 *
 * @summary Service interface for handling favorite command operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface FavoriteCommandUseCase {

    /**
     * Handles the creation of a new favorite.
     *
     * This method processes the command to create a new favorite,
     * validates the input, and persists the favorite data
     * Prevents duplicate favorites (same user + offer)
     *
     * @param command The command containing all necessary data for favorite creation
     * @return An Optional containing the created Favorite if successful, empty if failed
     * @throws IllegalArgumentException if the command contains invalid data
     */
    Optional<Favorite> handle(CreateFavoriteCommand command);

    /**
     * Handles the deletion of a favorite by its unique identifier
     *
     * This method processes the deletion of a favorite from the system
     *
     * @param id The unique identifier of the favorite to delete
     * @return true if the favorite was successfully deleted, false if not found
     * @throws IllegalArgumentException if the ID is invalid
     */
    boolean handleDelete(Long id);

    /**
     * Handles the deletion of a favorite by user ID and offer ID
     *
     * This method processes the deletion of a specific favorite relationship
     * between a user and an offer. Useful for un-hearting an offer
     *
     * @param command The command containing userId and offerId
     * @return true if the favorite was successfully deleted, false if not found
     * @throws IllegalArgumentException if the command contains invalid data
     */
    boolean handleDelete(DeleteFavoriteCommand command);
}
