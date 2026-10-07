package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignZoneException;

public record CampaignZone(ZoneType type, GeoPoint center, Integer radiusMeters, String district) {
    public static final int MIN_RADIUS_METERS = 400;
    public static final int MAX_RADIUS_METERS = 5_000;
    public static final int DISTRICT_COVERAGE_METERS = 2_000;

    public static CampaignZone radius(GeoPoint center, Integer radiusMeters) {
        if (center == null || radiusMeters == null || radiusMeters < MIN_RADIUS_METERS
                || radiusMeters > MAX_RADIUS_METERS) {
            throw InvalidCampaignZoneException.forRadius(MIN_RADIUS_METERS, MAX_RADIUS_METERS);
        }
        return new CampaignZone(ZoneType.RADIUS, center, radiusMeters, null);
    }

    public static CampaignZone district(String name, GeoPoint center) {
        if (name == null || name.isBlank() || center == null) {
            throw InvalidCampaignZoneException.forDistrict();
        }
        return new CampaignZone(ZoneType.DISTRICT, center, null, name.strip());
    }

    public boolean covers(GeoPoint point) {
        return isUnbounded() || center.distanceTo(point) <= reachMeters();
    }

    private boolean isUnbounded() {
        return center == null || (type == ZoneType.RADIUS && radiusMeters == null);
    }

    private int reachMeters() {
        return type == ZoneType.DISTRICT ? DISTRICT_COVERAGE_METERS : radiusMeters;
    }
}
