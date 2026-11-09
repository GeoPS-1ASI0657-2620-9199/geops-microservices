package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.ChangeCampaignStatusCommand;
import com.geopslabs.geops.catalog.domain.models.CampaignStatus;
import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignStatusException;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ChangeCampaignStatusRequest(@NotNull String status) {
    private static final Set<String> REQUESTABLE = Set.of(CampaignStatus.PAUSED.name(), CampaignStatus.ACTIVE.name());

    ChangeCampaignStatusCommand toCommand(Long campaignId, Long businessId) {
        if (!REQUESTABLE.contains(status)) {
            throw new InvalidCampaignStatusException();
        }
        return new ChangeCampaignStatusCommand(campaignId, businessId, CampaignStatus.valueOf(status));
    }
}
