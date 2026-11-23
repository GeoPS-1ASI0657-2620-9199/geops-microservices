package com.geopslabs.geops.catalog.infrastructure.web;

import java.time.LocalDate;
import java.util.List;

/** GET /api/v1/campaigns/metrics: every campaign of the business with its totals in the period (US11). */
public record CampaignMetricsSummaryResponse(CampaignMetricsResponse.Period period, List<CampaignTotals> campaigns) {

    public record CampaignTotals(Long campaignId, String name, String status, Validity validity,
                                 CampaignMetricsResponse.Totals totals, CampaignMetricsResponse.Rates rates) {
    }

    public record Validity(LocalDate start, LocalDate end) {
    }
}
