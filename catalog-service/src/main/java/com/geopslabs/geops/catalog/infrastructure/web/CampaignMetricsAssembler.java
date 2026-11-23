package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.CampaignMetrics;
import com.geopslabs.geops.catalog.application.usecases.CampaignMetricsSummary;
import com.geopslabs.geops.catalog.domain.models.MetricsPeriod;

final class CampaignMetricsAssembler {
    private CampaignMetricsAssembler() {
    }

    static CampaignMetricsResponse toResponse(CampaignMetrics metrics) {
        var campaign = metrics.campaign();
        var daily = metrics.daily().stream()
                .map(day -> new CampaignMetricsResponse.Day(day.date(), day.counts().impressions(),
                        day.counts().clicks(), day.counts().reservations(), day.counts().redemptions()))
                .toList();
        CampaignMetricsResponse.Baseline baseline = null;
        if (metrics.baseline() != null) {
            var declared = metrics.baseline();
            var comparison = metrics.comparison();
            baseline = new CampaignMetricsResponse.Baseline(declared.customersPerWeek(), declared.averageTicket(),
                    declared.declaredAt(), comparison.expectedCustomers(), comparison.campaignCustomers(),
                    comparison.customersChangePercent(), comparison.estimatedExtraSales());
        }
        return new CampaignMetricsResponse(campaign.getId(), campaign.getName(), campaign.getStatus().name(),
                periodOf(metrics.period()), CampaignMetricsResponse.Totals.of(metrics.totals()),
                CampaignMetricsResponse.Rates.of(metrics.totals()), daily, baseline);
    }

    static CampaignMetricsSummaryResponse toResponse(CampaignMetricsSummary summary) {
        var campaigns = summary.campaigns().stream()
                .map(row -> new CampaignMetricsSummaryResponse.CampaignTotals(row.campaign().getId(),
                        row.campaign().getName(), row.campaign().getStatus().name(),
                        new CampaignMetricsSummaryResponse.Validity(row.campaign().getPeriod().start(),
                                row.campaign().getPeriod().end()),
                        CampaignMetricsResponse.Totals.of(row.totals()), CampaignMetricsResponse.Rates.of(row.totals())))
                .toList();
        return new CampaignMetricsSummaryResponse(periodOf(summary.period()), campaigns);
    }

    private static CampaignMetricsResponse.Period periodOf(MetricsPeriod period) {
        return new CampaignMetricsResponse.Period(period.from(), period.to(), period.days());
    }
}
