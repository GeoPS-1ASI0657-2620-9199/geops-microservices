package com.geopslabs.geops.notification.domain.models.exceptions;

import com.geopslabs.geops.notification.shared.domain.RuleViolationException;

public class InvalidInboxSizeException extends RuleViolationException {
    private static final String CODE = "INVALID_REQUEST";
    private static final String MESSAGE = "La cantidad de avisos debe estar entre 1 y 50.";

    public InvalidInboxSizeException() {
        super(CODE, MESSAGE);
    }
}
