package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.shared.domain.ConflictException;

public class GeocodingRunInProgressException extends ConflictException {
    public static final String CODE = "GEOCODING_RUN_IN_PROGRESS";
    private static final String MESSAGE = "Ya hay una conversión a coordenadas en curso. Inténtalo cuando termine.";

    public GeocodingRunInProgressException() {
        super(CODE, MESSAGE);
    }
}
