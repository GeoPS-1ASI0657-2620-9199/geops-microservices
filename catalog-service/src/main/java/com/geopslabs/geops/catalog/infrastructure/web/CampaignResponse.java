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

    public record Zone(String type, Center center, Integer radiusMeters, String district) {
    }

    public record Center(Double latitude, Double longitude) {
    }

    public record Budget(BigDecimal amount, String currency) {
    }
}
