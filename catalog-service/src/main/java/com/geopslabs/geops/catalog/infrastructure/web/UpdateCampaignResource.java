package com.geopslabs.geops.catalog.infrastructure.web;

import java.time.LocalDate;

public record UpdateCampaignResource(String name, String description, LocalDate startDate,
                                     LocalDate endDate, String status, Float estimatedBudget,
                                     Long totalImpressions, Long totalClicks, Float ctr) {
}
