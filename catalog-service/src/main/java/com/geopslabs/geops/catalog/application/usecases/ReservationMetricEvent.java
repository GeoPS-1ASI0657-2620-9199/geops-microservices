package com.geopslabs.geops.catalog.application.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** What the campaign metrics read of ReservationCreated and ReservationRedeemed (US11). */
public record ReservationMetricEvent(UUID eventId, String eventType, Kind kind, Long offerId, Instant occurredAt) {
    public ReservationMetricEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(offerId);
        Objects.requireNonNull(occurredAt);
    }

    public enum Kind {
        RESERVED,
        REDEEMED
    }
}
