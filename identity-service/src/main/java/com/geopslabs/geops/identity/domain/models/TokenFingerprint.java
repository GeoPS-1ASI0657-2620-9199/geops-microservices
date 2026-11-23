package com.geopslabs.geops.identity.domain.models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 of the whole token. A revoked token is kept by its fingerprint, so the token itself is never stored and
 * the claims of the token do not change (US23 keeps them minimal, without jti).
 */
public record TokenFingerprint(String value) {
    private static final String ALGORITHM = "SHA-256";
    private static final int LENGTH = 64;

    public TokenFingerprint {
        if (value == null || value.length() != LENGTH) {
            throw new IllegalArgumentException("A token fingerprint has " + LENGTH + " hex characters");
        }
    }

    public static TokenFingerprint of(String token) {
        try {
            var digest = MessageDigest.getInstance(ALGORITHM).digest(token.getBytes(StandardCharsets.US_ASCII));
            return new TokenFingerprint(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(ALGORITHM + " is not available", exception);
        }
    }
}
