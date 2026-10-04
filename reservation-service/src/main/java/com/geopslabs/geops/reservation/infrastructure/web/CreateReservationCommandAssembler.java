package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;

/**
 * CreateReservationCommandAssembler
 *
 * Assembler class responsible for converting CreateReservationRequest objects
 * to CreateReservationCommand objects. This transformation follows the DDD pattern
 * of converting interface layer Resources to domain layer commands.
 *
 * @summary Converts CreateReservationRequest to CreateReservationCommand
 * @since 1.0
 * @author GeOps Labs
 */
public class CreateReservationCommandAssembler {

    /**
     * Converts a CreateReservationRequest to a CreateReservationCommand.
     *
     * This method transforms the REST API resource representation into
     * a domain command that can be processed by the domain services.
     * All validation is handled at the command level.
     *
     * @param resource The CreateReservationRequest from the REST API request
     * @return A CreateReservationCommand ready for domain processing
     * @throws IllegalArgumentException if the resource contains invalid data
     */
    public static CreateReservationCommand toCommandFromResource(CreateReservationRequest resource) {
        return new CreateReservationCommand(
            resource.userId(),
            resource.paymentId(),
            resource.paymentCode(),
            resource.productType(),
            resource.offerId(),
            resource.code(),
            resource.expiresAt()
        );
    }
}
