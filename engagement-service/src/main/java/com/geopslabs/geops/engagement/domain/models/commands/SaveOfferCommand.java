package com.geopslabs.geops.engagement.domain.models.commands;

public record SaveOfferCommand(
        Long consumerId,
        Long offerId
) {
    public SaveOfferCommand {
        if (consumerId == null) {
            throw new IllegalArgumentException("consumerId cannot be null or empty");
        }

        if (offerId == null) {
            throw new IllegalArgumentException("offerId cannot be null or empty");
        }
    }
}
