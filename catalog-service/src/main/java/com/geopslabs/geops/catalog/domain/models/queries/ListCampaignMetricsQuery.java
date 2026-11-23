package com.geopslabs.geops.catalog.domain.models.queries;

import java.time.LocalDate;

/** from and to are optional: without them the period is the last 30 days up to today. */
public record ListCampaignMetricsQuery(Long businessId, LocalDate from, LocalDate to) {
}
