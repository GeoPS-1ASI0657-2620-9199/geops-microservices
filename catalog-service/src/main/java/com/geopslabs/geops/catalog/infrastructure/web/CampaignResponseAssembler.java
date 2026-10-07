package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.PublishedCampaign;
import com.geopslabs.geops.catalog.domain.models.Campaign;
import com.geopslabs.geops.catalog.domain.models.Offer;

import java.util.Optional;

public final class CampaignResponseAssembler {

    private CampaignResponseAssembler() {
    }

    public static CampaignResponse toResponse(Campaign campaign) {
        var budget = new CampaignResponse.Budget(campaign.getEstimatedBudget().amount(),
                campaign.getEstimatedBudget().currency());
        return new CampaignResponse(campaign.getId(), campaign.getBusinessId(), campaign.getName(),
                campaign.getDescription(), periodOf(campaign), zoneOf(campaign), campaign.getStatus().name(), budget);
    }

    public static PublishedCampaignResponse toPublishedResponse(PublishedCampaign published) {
        var campaign = published.campaign();
        var offers = published.offers().stream().map(CampaignResponseAssembler::publishedOfferOf).toList();
        return new PublishedCampaignResponse(campaign.getId(), campaign.getBusinessId(), campaign.getName(),
                campaign.getStatus().name(), periodOf(campaign), zoneOf(campaign), offers);
    }

    private static CampaignResponse.Period periodOf(Campaign campaign) {
        return new CampaignResponse.Period(campaign.getPeriod().start(), campaign.getPeriod().end());
    }

    private static CampaignResponse.Zone zoneOf(Campaign campaign) {
        var zone = campaign.getZone();
        var center = Optional.ofNullable(zone.center())
                .map(point -> new CampaignResponse.Center(point.latitude(), point.longitude()))
                .orElse(null);
        return new CampaignResponse.Zone(zone.type().name(), center, zone.radiusMeters(), zone.district());
    }

    private static PublishedCampaignResponse.PublishedOffer publishedOfferOf(Offer offer) {
        return new PublishedCampaignResponse.PublishedOffer(offer.getId(), offer.getTitle(), offer.getValidTo(),
                offer.getStatus().name());
    }
}
