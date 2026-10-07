package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.RuleViolationException;

public class InvalidLocationException extends RuleViolationException {
    private static final String CODE = "INVALID_LOCATION";
    private static final String CORRECT_IT = " Corrige la ubicación del local.";
    private static final String OUT_OF_RANGE = "La %s debe estar entre %.0f y %.0f.";
    private static final String LATITUDE = "latitud";
    private static final String LONGITUDE = "longitud";
    private static final String MISSING_COORDINATES = "Falta la latitud o la longitud.";
    private static final String MISSING_ADDRESS = "Falta la dirección.";

    private InvalidLocationException(String reason) {
        super(CODE, reason + CORRECT_IT);
    }

    public static InvalidLocationException latitudeOutOfRange() {
        return new InvalidLocationException(
                OUT_OF_RANGE.formatted(LATITUDE, GeoPoint.MIN_LATITUDE, GeoPoint.MAX_LATITUDE));
    }

    public static InvalidLocationException longitudeOutOfRange() {
        return new InvalidLocationException(
                OUT_OF_RANGE.formatted(LONGITUDE, GeoPoint.MIN_LONGITUDE, GeoPoint.MAX_LONGITUDE));
    }

    public static InvalidLocationException missingCoordinates() {
        return new InvalidLocationException(MISSING_COORDINATES);
    }

    public static InvalidLocationException missingAddress() {
        return new InvalidLocationException(MISSING_ADDRESS);
    }
}
