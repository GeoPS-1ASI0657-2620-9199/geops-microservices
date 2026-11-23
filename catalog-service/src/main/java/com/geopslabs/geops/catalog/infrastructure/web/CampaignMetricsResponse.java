package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.MetricCounts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** GET /api/v1/campaigns/{id}/metrics (US11, US45). The rates are percentages; null when the base is 0. */
public record CampaignMetricsResponse(
        Long campaignId,
        String campaignName,
        String status,
        Period period,
        Totals totals,
        Rates rates,
        List<Day> daily,
        Baseline baseline) {

    public record Period(LocalDate from, LocalDate to, int days) {
    }

    public record Totals(long impressions, long clicks, long reservations, long redemptions) {
        static Totals of(MetricCounts counts) {
            return new Totals(counts.impressions(), counts.clicks(), counts.reservations(), counts.redemptions());
        }
    }

    public record Rates(BigDecimal clickThroughRate, BigDecimal reservationRate, BigDecimal redemptionRate,
                        BigDecimal conversionRate) {
        static Rates of(MetricCounts counts) {
            return new Rates(counts.clickThroughRate(), counts.reservationRate(), counts.redemptionRate(),
                    counts.conversionRate());
        }
    }

    public record Day(LocalDate date, long impressions, long clicks, long reservations, long redemptions) {
    }

    public record Baseline(int customersPerWeek, BigDecimal averageTicket, Instant declaredAt,
                           BigDecimal expectedCustomers, long campaignCustomers, BigDecimal customersChangePercent,
                           BigDecimal estimatedExtraSales) {
    }
}
