package com.geopslabs.geops.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferResponse(
        Long offerId,
        Long campaignId,
        Long businessId,
        String title,
        String conditions,
        BigDecimal price,
        LocalDate validTo,
        String category,
        String address,
        String imageUrl,
        String source,
        String sourceName,
        String status) {
}
