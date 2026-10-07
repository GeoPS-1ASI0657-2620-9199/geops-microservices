package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.Campaign;

public final class CampaignResponseAssembler {

    private CampaignResponseAssembler() {
    }

    public static CampaignResponse toResponse(Campaign campaign) {
        var period = new CampaignResponse.Period(campaign.getPeriod().start(), campaign.getPeriod().end());
        var zone = new CampaignResponse.Zone(campaign.getZone().type().name(), campaign.getZone().radiusMeters(),
                campaign.getZone().district());
        var budget = new CampaignResponse.Budget(campaign.getEstimatedBudget().amount(),
                campaign.getEstimatedBudget().currency());
        return new CampaignResponse(campaign.getId(), campaign.getBusinessId(), campaign.getName(),
                campaign.getDescription(), period, zone, campaign.getStatus().name(), budget);
    }
}
