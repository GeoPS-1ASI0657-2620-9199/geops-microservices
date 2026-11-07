package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidCampaignStatusException extends DomainException {
    private static final String CODE = "INVALID_CAMPAIGN_STATUS";
    private static final String MESSAGE = "El estado debe ser PAUSED o ACTIVE.";

    public InvalidCampaignStatusException() {
        super(CODE, MESSAGE);
    }
}
