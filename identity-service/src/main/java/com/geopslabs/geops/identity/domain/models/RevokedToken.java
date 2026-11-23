package com.geopslabs.geops.identity.domain.models;

import java.time.Instant;

/**
 * A token closed before its expiry (logout). It only needs to be kept until expiresAt: after that the token is
 * rejected for being expired.
 */
public record RevokedToken(TokenFingerprint fingerprint, Long userId, Instant expiresAt, Instant revokedAt) {

    public static RevokedToken of(VerifiedToken token, Instant revokedAt) {
        return new RevokedToken(token.fingerprint(), token.userId(), token.expiresAt(), revokedAt);
    }
}
