package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;

import java.util.Optional;

/**
 * SavedOfferCommandService
 *
 * Domain service interface that defines command operations for managing saved offers.
 * This service handles all write operations (Create, Delete) following the
 * Command Query Responsibility Segregation (CQRS) pattern
 *
 * @summary Service interface for handling saved offer command operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface SavedOfferCommandUseCase {

    /**
     * Handles the creation of a new saved offer.
     *
     * This method processes the command to create a new saved offer,
     * validates the input, and persists the saved offer data
     * Prevents duplicate saved offers (same user + offer)
     *
     * @param command The command containing all necessary data for saved offer creation
     * @return An Optional containing the created SavedOffer if successful, empty if failed
     * @throws IllegalArgumentException if the command contains invalid data
     */
    Optional<SavedOffer> handle(SaveOfferCommand command);

    /**
     * Handles the deletion of a saved offer by its unique identifier
     *
     * This method processes the deletion of a saved offer from the system
     *
     * @param id The unique identifier of the saved offer to delete
     * @return true if the saved offer was successfully deleted, false if not found
     * @throws IllegalArgumentException if the ID is invalid
     */
    boolean handleDelete(Long id);

    /**
     * Handles the deletion of a saved offer by user ID and offer ID
     *
     * This method processes the deletion of a specific saved offer relationship
     * between a user and an offer. Useful for un-hearting an offer
     *
     * @param command The command containing userId and offerId
     * @return true if the saved offer was successfully deleted, false if not found
     * @throws IllegalArgumentException if the command contains invalid data
     */
    boolean handleDelete(RemoveSavedOfferCommand command);
}
