package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidCampaignZoneException extends DomainException {
    public static final String CODE = "INVALID_CAMPAIGN_ZONE";
    private static final String MESSAGE = "El radio de la zona debe estar entre %d y %d metros y tener un centro.";

    public InvalidCampaignZoneException(int minRadiusMeters, int maxRadiusMeters) {
        super(CODE, MESSAGE.formatted(minRadiusMeters, maxRadiusMeters));
    }
}
