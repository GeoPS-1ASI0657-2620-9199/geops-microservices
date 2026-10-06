package com.geopslabs.geops.catalog.domain.models.queries;

public record GetAllOffersByCampaignIdQuery(Long campaignId) {
    public GetAllOffersByCampaignIdQuery{
        if(campaignId == null || campaignId < 1)
            throw new  IllegalArgumentException("Campaign Id cannot be null or less than 1");
    }
}
