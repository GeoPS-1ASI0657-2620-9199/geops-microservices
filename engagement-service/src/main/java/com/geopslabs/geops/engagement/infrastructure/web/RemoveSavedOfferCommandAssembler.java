package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.RemoveSavedOfferCommand;
import com.geopslabs.geops.engagement.infrastructure.web.RemoveSavedOfferRequest;

public class RemoveSavedOfferCommandAssembler {
    public static RemoveSavedOfferCommand toCommandFromResource(RemoveSavedOfferRequest resource) {
        return new RemoveSavedOfferCommand(
            resource.consumerId(),
            resource.offerId()
        );
    }
}

