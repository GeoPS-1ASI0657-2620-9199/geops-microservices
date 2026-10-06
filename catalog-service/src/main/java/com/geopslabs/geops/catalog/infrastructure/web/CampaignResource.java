package com.geopslabs.geops.catalog.infrastructure.web;

import java.time.LocalDate;

public record CampaignResource(Long id, Long businessId, String name, String description, LocalDate startDate, LocalDate endDate,
                               String status, float estimatedBudget) {
}
