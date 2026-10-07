package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.shared.domain.ForbiddenException;

public class TokenClaimsException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String MESSAGE = "Your token does not identify the account this operation needs";

    public TokenClaimsException() {
        super(CODE, MESSAGE);
    }
}
