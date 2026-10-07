package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;

public interface GetCampaignByIdUseCase {
    Campaign getById(GetCampaignByIdQuery query);
}
