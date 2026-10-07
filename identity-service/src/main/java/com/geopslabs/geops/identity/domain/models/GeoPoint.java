package com.geopslabs.geops.identity.domain.models;

public record GeoPoint(double latitude, double longitude) {
    public static final double MIN_LATITUDE = -90;
    public static final double MAX_LATITUDE = 90;
    public static final double MIN_LONGITUDE = -180;
    public static final double MAX_LONGITUDE = 180;

    public GeoPoint {
        if (!(latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE)) {
            throw InvalidLocationException.latitudeOutOfRange();
        }
        if (!(longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE)) {
            throw InvalidLocationException.longitudeOutOfRange();
        }
    }

    public static GeoPoint of(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw InvalidLocationException.missingCoordinates();
        }
        return new GeoPoint(latitude, longitude);
    }
}
