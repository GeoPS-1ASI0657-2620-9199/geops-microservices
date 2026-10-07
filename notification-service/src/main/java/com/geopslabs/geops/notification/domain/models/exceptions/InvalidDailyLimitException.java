package com.geopslabs.geops.notification.domain.models.exceptions;

import com.geopslabs.geops.notification.shared.domain.RuleViolationException;

public class InvalidDailyLimitException extends RuleViolationException {
    private static final String CODE = "INVALID_REQUEST";
    private static final String MESSAGE = "El máximo diario de avisos debe estar entre 1 y 10.";

    public InvalidDailyLimitException() {
        super(CODE, MESSAGE);
    }
}
