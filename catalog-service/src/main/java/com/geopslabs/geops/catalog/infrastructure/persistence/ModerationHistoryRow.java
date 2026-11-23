package com.geopslabs.geops.catalog.infrastructure.persistence;

import java.time.Instant;

public record ModerationHistoryRow(Long moderationId, Long offerId, String offerTitle, Long adminId, String reason,
                                   Instant removedAt) {
}
