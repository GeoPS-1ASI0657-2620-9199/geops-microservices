package com.geopslabs.geops.catalog.domain.models.commands;

public record UpdateOfferCommand (
        Long id,
        String title,
        Long businessId,
        java.math.BigDecimal price,
        java.time.LocalDate validTo,
        String location,
        String category,
        String imageUrl
) {
    public UpdateOfferCommand {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
    }
}
