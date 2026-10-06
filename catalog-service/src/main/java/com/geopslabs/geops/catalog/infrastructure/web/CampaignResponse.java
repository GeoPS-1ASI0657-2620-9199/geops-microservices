package com.geopslabs.geops.catalog.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignResponse(
        Long campaignId,
        Long businessId,
        String name,
        String description,
        Period period,
        Zone zone,
        String status,
        Budget estimatedBudget) {

    public record Period(LocalDate start, LocalDate end) {
    }

    public record Zone(String type, Integer radiusMeters, String district) {
    }

    public record Budget(BigDecimal amount, String currency) {
    }
}
