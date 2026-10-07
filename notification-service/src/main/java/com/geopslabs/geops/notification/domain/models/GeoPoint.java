package com.geopslabs.geops.notification.domain.models;

public record GeoPoint(Double latitude, Double longitude) {
    public static final double MIN_LATITUDE = -90;
    public static final double MAX_LATITUDE = 90;
    public static final double MIN_LONGITUDE = -180;
    public static final double MAX_LONGITUDE = 180;
    private static final String OUT_OF_RANGE_MESSAGE = "Latitude goes from %s to %s and longitude from %s to %s";

    public GeoPoint {
        if (!isWithin(latitude, MIN_LATITUDE, MAX_LATITUDE) || !isWithin(longitude, MIN_LONGITUDE, MAX_LONGITUDE)) {
            throw new IllegalArgumentException(OUT_OF_RANGE_MESSAGE.formatted(MIN_LATITUDE, MAX_LATITUDE,
                    MIN_LONGITUDE, MAX_LONGITUDE));
        }
    }

    private static boolean isWithin(Double value, double min, double max) {
        return value != null && value >= min && value <= max;
    }
}
