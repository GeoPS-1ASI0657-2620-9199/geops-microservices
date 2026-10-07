package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.ForbiddenException;

public class CampaignAccessDeniedException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String MESSAGE = "Campaign %d belongs to another account";

    public CampaignAccessDeniedException(Long campaignId) {
        super(CODE, MESSAGE.formatted(campaignId));
    }
}
