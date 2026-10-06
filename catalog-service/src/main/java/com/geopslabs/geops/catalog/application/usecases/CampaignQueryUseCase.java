package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllCampaignsByUserIdQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetAllCampaignsQuery;
import com.geopslabs.geops.catalog.domain.models.queries.GetCampaignByIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Campaign Query service interface to use all the queries
 */
public interface CampaignQueryUseCase {

    Optional<Campaign> handle(GetCampaignByIdQuery query);

    List<Campaign> handle(GetAllCampaignsQuery query);

    List<Campaign> handle(GetAllCampaignsByUserIdQuery query);
}
