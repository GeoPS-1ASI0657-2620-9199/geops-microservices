package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.BaselineComparison;
import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.DailyMetrics;
import com.geopslabs.geops.catalog.domain.models.MetricCounts;
import com.geopslabs.geops.catalog.domain.models.MetricsPeriod;

import java.util.List;

/**
 * The metrics of one campaign in a period: one entry per day, zeros included, their totals and, when the business
 * declared its baseline, the comparison against it (both null otherwise).
 */
public record CampaignMetrics(Campaign campaign, MetricsPeriod period, List<DailyMetrics> daily, MetricCounts totals,
                              BusinessBaseline baseline, BaselineComparison comparison) {
}
