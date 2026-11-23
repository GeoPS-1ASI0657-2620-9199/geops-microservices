package com.geopslabs.geops.identity.domain.ports;

import com.geopslabs.geops.identity.domain.models.RevokedToken;
import com.geopslabs.geops.identity.domain.models.TokenFingerprint;

import java.time.Instant;

public interface RevokedTokenRepositoryPort {
    boolean isRevoked(TokenFingerprint fingerprint);

    void save(RevokedToken revokedToken);

    /** Deletes the revocations of tokens that already expired; returns how many. */
    int deleteExpiredBefore(Instant instant);
}
