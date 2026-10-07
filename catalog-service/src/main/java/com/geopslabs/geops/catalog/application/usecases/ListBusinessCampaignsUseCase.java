package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignsByBusinessIdQuery;

import java.util.List;

public interface ListBusinessCampaignsUseCase {
    List<Campaign> list(GetCampaignsByBusinessIdQuery query);
}
