package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignZoneException;

public record CampaignZone(ZoneType type, GeoPoint center, Integer radiusMeters, String district) {
    public static final int MIN_RADIUS_METERS = 400;
    public static final int MAX_RADIUS_METERS = 5_000;

    public static CampaignZone radius(GeoPoint center, Integer radiusMeters) {
        if (center == null || radiusMeters == null || radiusMeters < MIN_RADIUS_METERS
                || radiusMeters > MAX_RADIUS_METERS) {
            throw new InvalidCampaignZoneException(MIN_RADIUS_METERS, MAX_RADIUS_METERS);
        }
        return new CampaignZone(ZoneType.RADIUS, center, radiusMeters, null);
    }
}
