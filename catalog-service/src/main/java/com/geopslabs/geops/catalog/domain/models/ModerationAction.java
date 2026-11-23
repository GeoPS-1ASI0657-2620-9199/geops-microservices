package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidModerationReasonException;

import java.time.Instant;
import java.util.Objects;

/**
 * An offer removed by an administrator (US24): who removed it, when and why. The row stays even if the offer is
 * never shown again, so the history answers who took it out of the catalog.
 */
public record ModerationAction(Long id, Long offerId, Long adminId, String reason, Instant removedAt) {
    public static final int MIN_REASON_LENGTH = 10;
    public static final int MAX_REASON_LENGTH = 255;

    public ModerationAction {
        Objects.requireNonNull(offerId);
        Objects.requireNonNull(adminId);
        Objects.requireNonNull(removedAt);
        reason = reason == null ? "" : reason.strip();
        if (reason.length() < MIN_REASON_LENGTH || reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidModerationReasonException(MIN_REASON_LENGTH, MAX_REASON_LENGTH);
        }
    }

    public ModerationAction withId(Long savedId) {
        return new ModerationAction(savedId, offerId, adminId, reason, removedAt);
    }
}
