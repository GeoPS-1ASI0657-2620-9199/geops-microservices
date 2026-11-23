package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.RuleViolationException;

public class InvalidEmailConfirmationException extends RuleViolationException {
    private static final String CODE = "INVALID_CONFIRMATION_LINK";
    private static final String MESSAGE = "El enlace de confirmación no es válido o ya venció.";

    public InvalidEmailConfirmationException() {
        super(CODE, MESSAGE);
    }
}
