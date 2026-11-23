package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.queries.ListCampaignMetricsQuery;

public interface ListCampaignMetricsUseCase {
    CampaignMetricsSummary list(ListCampaignMetricsQuery query);
}
