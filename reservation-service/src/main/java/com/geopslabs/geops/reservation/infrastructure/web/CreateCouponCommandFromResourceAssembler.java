package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.commands.CreateCouponCommand;

/**
 * CreateCouponCommandFromResourceAssembler
 *
 * Assembler class responsible for converting CreateCouponResource objects
 * to CreateCouponCommand objects. This transformation follows the DDD pattern
 * of converting interface layer Resources to domain layer commands.
 *
 * @summary Converts CreateCouponResource to CreateCouponCommand
 * @since 1.0
 * @author GeOps Labs
 */
public class CreateCouponCommandFromResourceAssembler {

    /**
     * Converts a CreateCouponResource to a CreateCouponCommand.
     *
     * This method transforms the REST API resource representation into
     * a domain command that can be processed by the domain services.
     * All validation is handled at the command level.
     *
     * @param resource The CreateCouponResource from the REST API request
     * @return A CreateCouponCommand ready for domain processing
     * @throws IllegalArgumentException if the resource contains invalid data
     */
    public static CreateCouponCommand toCommandFromResource(CreateCouponResource resource) {
        return new CreateCouponCommand(
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
