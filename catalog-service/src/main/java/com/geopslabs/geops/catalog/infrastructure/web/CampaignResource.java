package com.geopslabs.geops.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignResource(
        Long id,
        Long businessId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String zoneType,
        Integer radiusMeters,
        String district,
        String status,
        BigDecimal estimatedBudget) {
}
