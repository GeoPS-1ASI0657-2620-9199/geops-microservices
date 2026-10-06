package com.geopslabs.geops.catalog.domain.models.commands;

public record DeleteOfferCommand(Long id) {
    public DeleteOfferCommand {
        if(id == null || id < 1)
            throw new IllegalArgumentException("Offer Id cannot be null or less than 1");
    }
}
