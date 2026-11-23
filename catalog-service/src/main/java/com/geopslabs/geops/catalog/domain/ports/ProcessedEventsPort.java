package com.geopslabs.geops.catalog.domain.ports;

import java.time.Instant;
import java.util.UUID;

/** processed_events for a consumer other than the reputation of the business: each one discards its own repeats. */
public interface ProcessedEventsPort {
    /** Records the event for the consumer; false when it was already recorded. Runs in the caller's transaction. */
    boolean markProcessed(UUID eventId, String eventType, String consumer, Instant processedAt);
}
