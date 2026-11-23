package com.geopslabs.geops.identity.domain.models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The pending confirmation of the email of a user (US28). Only the SHA-256 of the link token is kept, so the rows
 * of this table do not let anyone confirm an account; the token itself only travels in the UserRegistered event.
 */
public record EmailConfirmation(Long userId, String tokenHash, Instant expiresAt, Instant createdAt) {
    public static final Duration VALIDITY = Duration.ofHours(24);
    private static final String HASH_ALGORITHM = "SHA-256";

    public EmailConfirmation {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(expiresAt);
        Objects.requireNonNull(createdAt);
    }

    public static EmailConfirmation issue(Long userId, String token, Instant now) {
        return new EmailConfirmation(userId, hashOf(token), now.plus(VALIDITY), now);
    }

    public static String hashOf(String token) {
        try {
            var digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(HASH_ALGORITHM + " is not available", exception);
        }
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
