package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.commands.UpdateOfferCommand;
import com.geopslabs.geops.catalog.infrastructure.web.UpdateOfferResource;

/**
 * UpdateOfferCommandFromResourceAssembler
 * Assembler class responsible for converting UpdateOfferResource objects
 * to UpdateOfferCommand objects. This transformation follows the DDD pattern
 * of converting interface layer Resources to domain layer commands
 *
 * @summary Converts UpdateOfferResource to UpdateOfferCommand
 * @since 1.0
 * @author GeOps Labs
 */
public class UpdateOfferCommandFromResourceAssembler {

    /**
     * Converts an UpdateOfferResource to an UpdateOfferCommand
     * This method transforms the REST API resource representation into
     * a domain command that can be processed by the domain services
     * The ID is provided separately from the path variable
     * All validation is handled at the command level
     *
     * @param id The offer ID from the path variable
     * @param resource The UpdateOfferResource from the REST API request
     * @return An UpdateOfferCommand ready for domain processing
     * @throws IllegalArgumentException if the resource contains invalid data
     */
    public static UpdateOfferCommand toCommandFromResource(Long id, UpdateOfferResource resource) {
        return new UpdateOfferCommand(
            id,
            resource.title(),
            resource.businessId(),
            resource.price(),
            resource.codePrefix(),
            resource.validTo(),
            resource.rating(),
            resource.location(),
            resource.category(),
            resource.imageUrl()
        );
    }
}

