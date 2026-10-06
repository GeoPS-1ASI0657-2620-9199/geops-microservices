package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.SaveOfferCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaveOfferRequest(@NotNull @Positive Long offerId) {

    public SaveOfferCommand toCommand(Long consumerId) {
        return new SaveOfferCommand(consumerId, offerId);
    }
}
