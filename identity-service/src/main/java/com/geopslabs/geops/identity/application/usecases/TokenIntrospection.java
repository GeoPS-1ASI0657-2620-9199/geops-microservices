package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.VerifiedToken;

/**
 * Answer of the token validation (US26): an active token carries who sent it and until when it is valid; an
 * inactive one carries nothing, so the answer never says why it was rejected.
 */
public record TokenIntrospection(boolean active, VerifiedToken token) {
    private static final TokenIntrospection INACTIVE = new TokenIntrospection(false, null);

    public static TokenIntrospection activeFor(VerifiedToken token) {
        return new TokenIntrospection(true, token);
    }

    public static TokenIntrospection inactive() {
        return INACTIVE;
    }
}
