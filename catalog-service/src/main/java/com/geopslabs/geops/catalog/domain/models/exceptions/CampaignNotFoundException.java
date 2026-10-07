package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.NotFoundException;

public class CampaignNotFoundException extends NotFoundException {
    private static final String CODE = "CAMPAIGN_NOT_FOUND";
    private static final String MESSAGE = "Campaign %d was not found";

    public CampaignNotFoundException(Long campaignId) {
        super(CODE, MESSAGE.formatted(campaignId));
    }
}
