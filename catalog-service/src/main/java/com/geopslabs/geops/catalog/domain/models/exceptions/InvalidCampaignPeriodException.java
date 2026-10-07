package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidCampaignPeriodException extends DomainException {
    public static final String CODE = "INVALID_CAMPAIGN_PERIOD";
    private static final String MESSAGE = "La fecha de fin de la campaña es anterior a su fecha de inicio.";

    public InvalidCampaignPeriodException() {
        super(CODE, MESSAGE);
    }
}
