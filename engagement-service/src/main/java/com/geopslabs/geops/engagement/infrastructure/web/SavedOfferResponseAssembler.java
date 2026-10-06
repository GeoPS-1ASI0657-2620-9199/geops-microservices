package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.SavedOffer;
import com.geopslabs.geops.engagement.infrastructure.web.SavedOfferResponse;

/**
 * SavedOfferResponseAssembler
 *
 * Assembler class responsible for converting SavedOffer entity objects
 * to SavedOfferResponse objects. This transformation follows the DDD pattern
 * of converting domain layer entities to interface layer Resources for API responses
 *
 * @summary Converts SavedOffer entity to SavedOfferResponse
 * @since 1.0
 * @author GeOps Labs
 */
public class SavedOfferResponseAssembler {

    /**
     * Converts a SavedOffer entity to a SavedOfferResponse
     *
     * This method transforms the domain entity representation into
     * a REST API resource that can be returned in HTTP responses
     * It extracts all relevant saved offer information for client consumption
     *
     * @param entity The SavedOffer entity from the domain layer
     * @return A SavedOfferResponse ready for REST API response
     */
    public static SavedOfferResponse toResourceFromEntity(SavedOffer entity) {
        return new SavedOfferResponse(
                entity.getId(),
                entity.getConsumerId(),
                entity.getOfferId(),
                entity.getSavedAt() != null ? entity.getSavedAt().toString() : null
        );
    }
}
