package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;

public final class SaveOfferCommandAssembler {

    private SaveOfferCommandAssembler() {
    }

    public static SaveOfferCommand toCommand(AuthenticatedUser user, SaveOfferRequest request) {
        return new SaveOfferCommand(user.consumerId(), request.offerId());
    }
}
