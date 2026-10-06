package com.geopslabs.geops.catalog.domain.models.commands;

import java.math.BigDecimal;

public record CreateOfferCommand (
        Long campaignId,
        String title,
        Long businessId,
        java.math.BigDecimal price,
        java.time.LocalDate validTo,
        String location,
        String category,
        String imageUrl
){
    public CreateOfferCommand {

        if(campaignId == null || campaignId < 1){
            throw new IllegalArgumentException("campaignId cannot be null or less than 1");
        }

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
