package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.backend.offers.domain.model.aggregates.Offer;
import com.geopslabs.geops.backend.offers.interfaces.rest.transform.OfferResourceFromEntityAssembler;
import com.geopslabs.geops.backend.offers.interfaces.rest.resources.OfferResource;

/**
 * ReservationResponseAssembler
 *
 * Assembler class responsible for converting Reservation entity objects
 * to ReservationResponse objects. This transformation follows the DDD pattern
 * of converting domain layer entities to interface layer Resources for API responses.
 *
 * @summary Converts Reservation entity to ReservationResponse
 * @since 1.0
 * @author GeOps Labs
 */
public class ReservationResponseAssembler {

    /**
     * Converts a Reservation entity to a ReservationResponse without embedded offer data.
     *
     * This method transforms the domain entity representation into
     * a REST API resource that can be returned in HTTP responses.
     * It extracts all relevant reservation information for client consumption.
     *
     * @param entity The Reservation entity from the domain layer
     * @return A ReservationResponse ready for REST API response
     */
    public static ReservationResponse toResourceFromEntity(Reservation entity) {
        return toResourceFromEntityWithOffer(entity, null);
    }

    /**
     * Converts a Reservation entity to a ReservationResponse and embeds Offer data when provided.
     *
     * This method transforms the domain entity representation into
     * a REST API resource that can be returned in HTTP responses.
     * It extracts all relevant reservation information for client consumption,
     * and if an Offer is provided, it embeds the corresponding offer data
     * into the ReservationResponse.
     *
     * @param entity The Reservation entity from the domain layer
     * @param offer  Optional Offer aggregate to embed inside the resource (may be null)
     * @return A ReservationResponse ready for REST API response with optional offer
     */
    public static ReservationResponse toResourceFromEntityWithOffer(Reservation entity, Offer offer) {
        OfferResource offerResource = null;
        if (offer != null) {
            offerResource = OfferResourceFromEntityAssembler.toResourceFromEntity(offer);
        }

        return new ReservationResponse(
            entity.getId(),
            entity.getUserId(),
            entity.getPaymentId(),
            entity.getPaymentCode(),
            entity.getProductType(),
            entity.getOfferId(),
            offerResource,
            entity.getCode(),
            entity.getExpiresAt(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }
}
