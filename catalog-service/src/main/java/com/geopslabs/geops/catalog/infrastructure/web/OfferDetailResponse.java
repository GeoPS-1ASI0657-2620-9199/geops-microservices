package com.geopslabs.geops.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferDetailResponse(
        Long offerId,
        String title,
        String conditions,
        BigDecimal price,
        LocalDate validTo,
        String category,
        String address,
        Double latitude,
        Double longitude,
        String imageUrl,
        String source,
        Long businessId,
        String businessName,
        boolean verifiedSeal,
        boolean available) {
}
