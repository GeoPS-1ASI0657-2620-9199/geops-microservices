package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.shared.domain.ForbiddenException;

public class TokenClaimsException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String MESSAGE = "Your token does not identify the account this operation needs";

    public TokenClaimsException() {
        super(CODE, MESSAGE);
    }
}
