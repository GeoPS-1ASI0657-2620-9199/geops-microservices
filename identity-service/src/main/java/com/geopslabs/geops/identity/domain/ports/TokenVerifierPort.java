package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.VerifiedToken;

import java.util.Optional;

public interface TokenVerifierPort {
    /** Empty when the token is malformed, altered, expired or issued by someone else. */
    Optional<VerifiedToken> verify(String token);
}
