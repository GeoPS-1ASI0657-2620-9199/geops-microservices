package com.geopslabs.geops.catalog.domain.models;

import java.time.Instant;

/** One row of the moderation history: the action and the title of the offer it removed. */
public record ModerationEntry(Long moderationId, Long offerId, String offerTitle, Long adminId, String reason,
                              Instant removedAt) {
}
