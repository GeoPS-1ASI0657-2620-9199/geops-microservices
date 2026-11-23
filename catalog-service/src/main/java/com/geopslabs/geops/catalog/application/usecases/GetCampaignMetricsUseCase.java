package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignMetricsQuery;

public interface GetCampaignMetricsUseCase {
    CampaignMetrics getMetrics(GetCampaignMetricsQuery query);
}
