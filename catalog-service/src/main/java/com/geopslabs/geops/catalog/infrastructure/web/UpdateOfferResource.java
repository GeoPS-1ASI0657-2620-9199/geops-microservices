package com.geopslabs.geops.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateOfferResource(
    String title,
    Long businessId,
    BigDecimal price,
    LocalDate validTo,
    String location,
    String category,
    String imageUrl
) {
    public UpdateOfferResource {
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be positive if provided");
        }

        if (title != null && title.isBlank()) {
            throw new IllegalArgumentException("title cannot be blank if provided");
        }

        if (businessId != null && businessId < 1) {
            throw new IllegalArgumentException("businessId cannot be less than 1 if provided");
        }

        if (location != null && location.isBlank()) {
            throw new IllegalArgumentException("location cannot be blank if provided");
        }

        if (category != null && category.isBlank()) {
            throw new IllegalArgumentException("category cannot be blank if provided");
        }
    }
}
