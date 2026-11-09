package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.shared.ForbiddenException;

public class TokenClaimsException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String MESSAGE = "Tu sesión no identifica la cuenta que necesita esta operación.";

    public TokenClaimsException() {
        super(CODE, MESSAGE);
    }
}
