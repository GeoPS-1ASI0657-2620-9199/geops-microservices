package com.geopslabs.geops.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateOfferResource(
    Long campaignId,
    String title,
    Long businessId,
    BigDecimal price,
    LocalDate validTo,
    String location,
    String category,
    String imageUrl)
{
    public CreateOfferResource {
        if(campaignId == null || campaignId < 0)
            throw new IllegalArgumentException("Campaign id cannot be null or negative");

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be null or empty");
        }

        if (businessId == null || businessId < 1) {
            throw new IllegalArgumentException("businessId cannot be null or less than 1");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be positive");
        }

        if (validTo == null) {
            throw new IllegalArgumentException("validTo cannot be null");
        }

        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location cannot be null or empty");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category cannot be null or empty");
        }
    }
}
