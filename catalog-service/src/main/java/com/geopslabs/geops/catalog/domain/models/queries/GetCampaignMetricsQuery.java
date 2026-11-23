package com.geopslabs.geops.catalog.domain.models.queries;

import java.time.LocalDate;

/** from and to are optional: without them the period is the validity of the campaign up to today. */
public record GetCampaignMetricsQuery(Long campaignId, Long businessId, LocalDate from, LocalDate to) {
}
