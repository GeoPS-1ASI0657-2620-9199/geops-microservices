package com.geopslabs.geops.notification.domain.models;

import java.time.Instant;
import java.util.Objects;

/**
 * The confirmation link that Identity issued for a new account (US28). It arrives in UserRegistered; Notification
 * only delivers it by email and never stores the token.
 */
public record ConfirmationLink(Long userId, String email, String token, Instant expiresAt) {
    public ConfirmationLink {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(email);
        Objects.requireNonNull(token);
        Objects.requireNonNull(expiresAt);
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
