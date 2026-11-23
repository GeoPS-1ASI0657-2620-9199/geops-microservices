package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidMetricsPeriodException extends DomainException {
    public static final String CODE = "INVALID_METRICS_PERIOD";

    private InvalidMetricsPeriodException(String message) {
        super(CODE, message);
    }

    public static InvalidMetricsPeriodException endBeforeStart() {
        return new InvalidMetricsPeriodException("La fecha final del periodo es anterior a la fecha inicial.");
    }

    public static InvalidMetricsPeriodException tooLong(int maxDays) {
        return new InvalidMetricsPeriodException("El periodo puede abarcar como máximo %d días.".formatted(maxDays));
    }
}
