package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidGeoPointException extends DomainException {
    public static final String CODE = "INVALID_COORDINATES";
    private static final String MESSAGE = "La latitud debe estar entre -90 y 90 y la longitud entre -180 y 180";

    public InvalidGeoPointException() {
        super(CODE, MESSAGE);
    }
}
