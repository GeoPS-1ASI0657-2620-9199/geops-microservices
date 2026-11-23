package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.MetricCounts;
import com.geopslabs.geops.catalog.domain.models.MetricsPeriod;

import java.util.List;

/** Every campaign of the business with its totals in the period; one without activity comes with zeros. */
public record CampaignMetricsSummary(MetricsPeriod period, List<CampaignTotals> campaigns) {

    public record CampaignTotals(Campaign campaign, MetricCounts totals) {
    }
}
