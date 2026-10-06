package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import com.geopslabs.geops.engagement.infrastructure.web.SaveOfferRequest;

public class SaveOfferCommandAssembler {
    public static SaveOfferCommand toCommandFromResource(SaveOfferRequest resource) {
        return new SaveOfferCommand(
            resource.consumerId(),
            resource.offerId()
        );
    }
}

