package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.CampaignStatus;

public record ChangeCampaignStatusCommand(Long campaignId, Long businessId, CampaignStatus status) {
}
