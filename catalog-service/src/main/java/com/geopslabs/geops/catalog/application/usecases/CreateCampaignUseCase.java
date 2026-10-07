package com.geopslabs.geops.catalog.application.usecases;

public interface CreateCampaignUseCase {
    PublishedCampaign publish(CreateCampaignCommand command);
}
