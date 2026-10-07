package com.geopslabs.geops.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NearbyOfferResponse(
        Long offerId,
        String title,
        Long businessId,
        String businessName,
        boolean verifiedSeal,
        long distanceMeters,
        int walkMinutes,
        String category,
        BigDecimal price,
        LocalDate validTo) {
}
