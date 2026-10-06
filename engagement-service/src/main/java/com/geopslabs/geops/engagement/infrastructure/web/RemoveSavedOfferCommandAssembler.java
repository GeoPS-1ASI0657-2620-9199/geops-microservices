package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.infrastructure.web.RemoveSavedOfferRequest;

/**
 * RemoveSavedOfferCommandAssembler
 *
 * Assembler class to transform RemoveSavedOfferRequest into RemoveSavedOfferCommand
 * This class follows the Assembler pattern to separate REST resources from domain commands
 *
 * @summary Assembler for converting delete saved offer resource to command
 * @since 1.0
 * @author GeOps Labs
 */
public class RemoveSavedOfferCommandAssembler {

    /**
     * Converts a RemoveSavedOfferRequest to a RemoveSavedOfferCommand
     *
     * @param resource The delete saved offer resource from REST request
     * @return A RemoveSavedOfferCommand ready for processing by the service layer
     * @throws IllegalArgumentException if resource contains invalid data
     */
    public static RemoveSavedOfferCommand toCommandFromResource(RemoveSavedOfferRequest resource) {
        return new RemoveSavedOfferCommand(
            resource.userId(),
            resource.offerId()
        );
    }
}

