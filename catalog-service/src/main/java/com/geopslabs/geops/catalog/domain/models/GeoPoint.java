package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidGeoPointException;

public record GeoPoint(double latitude, double longitude) {
    public static final double MIN_LATITUDE = -90;
    public static final double MAX_LATITUDE = 90;
    public static final double MIN_LONGITUDE = -180;
    public static final double MAX_LONGITUDE = 180;
    public static final double EARTH_MEAN_RADIUS_METERS = 6_371_008.7714;

    public GeoPoint {
        if (!isValid(latitude, longitude)) {
            throw new InvalidGeoPointException();
        }
    }

    public static boolean isValid(double latitude, double longitude) {
        return latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE
                && longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
    }

    public double distanceTo(GeoPoint other) {
        var deltaLatitude = Math.toRadians(other.latitude - latitude);
        var deltaLongitude = Math.toRadians(other.longitude - longitude);
        var haversine = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(deltaLongitude / 2), 2);
        return 2 * EARTH_MEAN_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
    }
}
