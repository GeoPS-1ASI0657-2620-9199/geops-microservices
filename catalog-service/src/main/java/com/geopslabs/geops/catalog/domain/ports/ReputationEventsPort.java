package com.geopslabs.geops.catalog.domain.ports;

import java.time.Instant;
import java.util.UUID;

/**
 * processed_events and standing_changes: what Catalog already applied from other contexts, and every time the seal of
 * a business changed.
 */
public interface ReputationEventsPort {
    /** Records the event; false when it was already recorded (a redelivery). Runs in the caller's transaction. */
    boolean markProcessed(UUID eventId, String eventType, Instant processedAt);

    void recordSealChange(Long businessId, boolean verifiedSeal, String reason, Instant changedAt);
}
