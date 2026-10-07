package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.shared.domain.ForbiddenException;

public class TokenClaimsException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String MESSAGE = "Your token does not identify the account this operation needs";

    public TokenClaimsException() {
        super(CODE, MESSAGE);
    }
}
