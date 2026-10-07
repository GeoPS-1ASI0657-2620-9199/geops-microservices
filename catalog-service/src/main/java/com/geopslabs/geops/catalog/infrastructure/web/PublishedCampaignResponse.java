package com.geopslabs.geops.catalog.infrastructure.web;

import java.time.LocalDate;
import java.util.List;

public record PublishedCampaignResponse(
        Long campaignId,
        Long businessId,
        String name,
        String status,
        CampaignResponse.Period period,
        CampaignResponse.Zone zone,
        List<PublishedOffer> offers) {

    public record PublishedOffer(Long offerId, String title, LocalDate validTo, String status) {
    }
}
