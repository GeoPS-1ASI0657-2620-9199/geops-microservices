package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.ForbiddenException;

/**
 * The token belongs to another business. It answers the same whether or not the profile exists.
 */
public class BusinessProfileAccessDeniedException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String MESSAGE = "Solo el dueño del negocio puede hacer esta operación.";

    public BusinessProfileAccessDeniedException() {
        super(CODE, MESSAGE);
    }
}
