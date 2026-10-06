package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.RuleViolationException;

public class RoleNotAllowedException extends RuleViolationException {
    private static final String CODE = "ROLE_NOT_ALLOWED";
    private static final String MESSAGE = "Ese tipo de cuenta no se puede crear desde el registro.";

    public RoleNotAllowedException() {
        super(CODE, MESSAGE);
    }
}
