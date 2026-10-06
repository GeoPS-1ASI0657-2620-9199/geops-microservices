package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.commands.CreateCampaignCommand;
import com.geopslabs.geops.catalog.domain.models.commands.DeleteCampaignCommand;
import com.geopslabs.geops.catalog.domain.models.commands.UpdateCampaignCommand;

import java.util.Optional;

/**
 * Campaign Command service interface to use all the commands
 */
public interface CampaignCommandUseCase {

    Optional<Campaign> handle(CreateCampaignCommand command);

    Optional<Campaign> handle(UpdateCampaignCommand command);

    boolean handle(DeleteCampaignCommand command);
}
