package com.geopslabs.geops.engagement.domain.models.commands;

public record RemoveSavedOfferCommand(
    Long consumerId,
    Long offerId
) {
    public RemoveSavedOfferCommand {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null");
        }

        if (offerId == null) {
            throw new IllegalArgumentException("offerId cannot be null");
        }
    }
}

