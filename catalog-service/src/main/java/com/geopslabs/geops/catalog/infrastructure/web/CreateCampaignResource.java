package com.geopslabs.geops.catalog.infrastructure.web;

import java.time.LocalDate;

public record CreateCampaignResource(Long businessId, String name, String description, LocalDate startDate, LocalDate endDate, Float estimatedBudget) {
}
