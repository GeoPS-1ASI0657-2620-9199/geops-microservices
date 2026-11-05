package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Campaign;

public interface ChangeCampaignStatusUseCase {
    Campaign changeStatus(ChangeCampaignStatusCommand command);
}
