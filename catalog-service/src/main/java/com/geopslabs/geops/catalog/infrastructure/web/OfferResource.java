package com.geopslabs.geops.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferResource(
    Long id,
    Long campaignId,
    String title,
    Long businessId,
    BigDecimal price,
    LocalDate validTo,
    String location,
    String category,
    String imageUrl
) {
}
