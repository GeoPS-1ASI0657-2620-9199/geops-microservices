package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidCampaignZoneException extends DomainException {
    public static final String CODE = "INVALID_CAMPAIGN_ZONE";
    private static final String RADIUS_MESSAGE = "El radio de la zona debe estar entre %d y %d metros y tener un centro.";
    private static final String DISTRICT_MESSAGE = "La zona por distrito necesita el nombre del distrito y su centro.";

    private InvalidCampaignZoneException(String message) {
        super(CODE, message);
    }

    public static InvalidCampaignZoneException forRadius(int minRadiusMeters, int maxRadiusMeters) {
        return new InvalidCampaignZoneException(RADIUS_MESSAGE.formatted(minRadiusMeters, maxRadiusMeters));
    }

    public static InvalidCampaignZoneException forDistrict() {
        return new InvalidCampaignZoneException(DISTRICT_MESSAGE);
    }
}
