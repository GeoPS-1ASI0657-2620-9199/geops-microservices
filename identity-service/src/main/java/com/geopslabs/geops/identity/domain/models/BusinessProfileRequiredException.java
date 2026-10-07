package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.RuleViolationException;

public class BusinessProfileRequiredException extends RuleViolationException {
    private static final String CODE = "BUSINESS_PROFILE_REQUIRED";
    private static final String MESSAGE = "Completa los datos del negocio para crear la cuenta.";

    public BusinessProfileRequiredException() {
        super(CODE, MESSAGE);
    }
}
