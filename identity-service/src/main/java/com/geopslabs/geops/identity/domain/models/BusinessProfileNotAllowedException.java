package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.RuleViolationException;

public class BusinessProfileNotAllowedException extends RuleViolationException {
    private static final String CODE = "BUSINESS_PROFILE_NOT_ALLOWED";
    private static final String MESSAGE = "Los datos del negocio solo van en una cuenta de negocio.";

    public BusinessProfileNotAllowedException() {
        super(CODE, MESSAGE);
    }
}
