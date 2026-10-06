package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.infrastructure.web.SaveOfferRequest;

/**
 * SaveOfferCommandAssembler
 *
 * Assembler class responsible for converting SaveOfferRequest objects
 * to SaveOfferCommand objects. This transformation follows the DDD pattern
 * of converting interface layer Resources to domain layer commands
 *
 * @summary Converts SaveOfferRequest to SaveOfferCommand
 * @since 1.0
 * @author GeOps Labs
 */
public class SaveOfferCommandAssembler {

    /**
     * Converts a SaveOfferRequest to a SaveOfferCommand
     *
     * This method transforms the REST API resource representation into
     * a domain command that can be processed by the domain services
     * All validation is handled at the command level
     *
     * @param resource The SaveOfferRequest from the REST API request
     * @return A SaveOfferCommand ready for domain processing
     * @throws IllegalArgumentException if the resource contains invalid data
     */
    public static SaveOfferCommand toCommandFromResource(SaveOfferRequest resource) {
        return new SaveOfferCommand(
            resource.userId(),
            resource.offerId()
        );
    }
}

