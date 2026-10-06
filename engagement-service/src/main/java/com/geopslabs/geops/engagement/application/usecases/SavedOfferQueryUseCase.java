package com.geopslabs.geops.engagement.application.usecases;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOfferByIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOfferByUserIdAndOfferIdQuery;
import com.geopslabs.geops.engagement.domain.models.queries.GetSavedOffersByConsumerQuery;

import java.util.List;
import java.util.Optional;

/**
 * SavedOfferQueryService
 *
 * Domain service interface that defines query operations for saved offers.
 * This service handles all read operations following the Command Query Responsibility
 * Segregation (CQRS) pattern, providing various ways to retrieve saved offer data
 *
 * @summary Service interface for handling saved offer query operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface SavedOfferQueryUseCase {

    /**
     * Handles the query to retrieve all saved offers for a specific user
     *
     * This method processes the query to find all saved offers associated with a given userId
     * Used to display the user's list of saved offer offers
     * Endpoint: GET /api/v1/saved-offers?userId={id}
     *
     * @param query The query containing the userId
     * @return A list of SavedOffer objects associated with the user
     * @throws IllegalArgumentException if the query contains invalid data
     */
    List<SavedOffer> handle(GetSavedOffersByConsumerQuery query);

    /**
     * Handles the query to retrieve a saved offer by user ID and offer ID
     *
     * This method processes the query to find a specific saved offer based on the
     * combination of userId and offerId. Useful for checking if an offer is already
     * saved offer by the user
     *
     * @param query The query containing the userId and offerId
     * @return An Optional containing the SavedOffer if found, empty otherwise
     * @throws IllegalArgumentException if the query contains invalid data
     */
    Optional<SavedOffer> handle(GetSavedOfferByUserIdAndOfferIdQuery query);

    /**
     * Handles the query to retrieve a saved offer by its unique identifier
     *
     * This method processes the query to find a saved offer based on its ID
     * Useful for operations that require direct access to a saved offer entity
     *
     * @param query The query containing the saved offer ID
     * @return An Optional containing the SavedOffer if found, empty otherwise
     * @throws IllegalArgumentException if the query contains invalid data
     */
    Optional<SavedOffer> handle(GetSavedOfferByIdQuery query);
}


