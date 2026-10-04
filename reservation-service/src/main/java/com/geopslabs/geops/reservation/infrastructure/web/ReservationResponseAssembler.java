package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.Reservation;

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

    public static ReservationResponse toResourceFromEntity(Reservation entity) {
        return new ReservationResponse(
            entity.getId(),
            entity.getConsumerId(),
            entity.getPaymentId(),
            entity.getPaymentCode(),
            entity.getProductType(),
            entity.getOfferId(),
            entity.getCode(),
            entity.getExpiresAt()
        );
    }
}
