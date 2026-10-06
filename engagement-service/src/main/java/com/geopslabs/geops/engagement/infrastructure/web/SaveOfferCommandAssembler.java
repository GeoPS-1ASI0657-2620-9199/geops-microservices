package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;

public final class SaveOfferCommandAssembler {

    private SaveOfferCommandAssembler() {
    }

    public static SaveOfferCommand toCommandFromResource(SaveOfferRequest request) {
        return new SaveOfferCommand(request.consumerId(), request.offerId());
    }
}
