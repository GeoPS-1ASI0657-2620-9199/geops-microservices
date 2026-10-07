package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidSearchRadiusException extends DomainException {
    public static final String CODE = "RADIUS_OUT_OF_RANGE";
    private static final String MESSAGE = "El radio debe estar entre %d y %d minutos a pie";

    public InvalidSearchRadiusException(int minMinutes, int maxMinutes) {
        super(CODE, MESSAGE.formatted(minMinutes, maxMinutes));
    }
}
