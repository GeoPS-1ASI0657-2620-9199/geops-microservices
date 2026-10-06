package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllOffersByCampaignIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllOffersQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOfferByIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetOffersByIdsQuery;

import java.util.List;
import java.util.Optional;

/**
 * OfferQueryUseCase
 * Domain service interface that defines query operations for offers.
 * This service handles all GET operations
 *
 * @summary Service interface for handling offer query operations
 * @since 4.0
 * @author GeOps Labs
 */
public interface OfferQueryUseCase {

    /**
     * Handles the creation of a list containing all the offers in the database
     * @param query Contains nothing (CQRS design)
     * @return A list with all the offers from the database
     */
    List<Offer> handle(GetAllOffersQuery query);

    /**
     * Function to get an offer from the database using its id
     * @param query The query containing the offer unique id
     * @return An offer with the given ID
     */
    Optional<Offer> handle(GetOfferByIdQuery query);

    /**
     * Handles the creation of a list containing offers from a list of ids
     * @param query The query containing the list of ids
     * @return A list of offers from the given list of ids
     */
    List<Offer> handle(GetOffersByIdsQuery query);

    /**
     * Handles the creation of a list containing offers from a campaign
     * @param query The query containing the campaign id
     * @return A list of offers from a campaign
     */
    List<Offer> handle(GetAllOffersByCampaignIdQuery query);
}
