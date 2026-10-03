package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.RuleViolationException;

public class InvalidRucException extends RuleViolationException {
    private static final String CODE = "INVALID_RUC";
    private static final String MESSAGE = "El RUC debe tener %d dígitos.".formatted(Ruc.RUC_LENGTH);

    public InvalidRucException() {
        super(CODE, MESSAGE);
    }
}
