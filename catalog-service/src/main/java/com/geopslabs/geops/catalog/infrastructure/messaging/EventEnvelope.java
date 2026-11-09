package com.geopslabs.geops.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * The envelope of every event (BRD-11, 7.1). {@code correlationId} is optional: it is the X-Correlation-ID of the
 * request that caused the event, so a flow can be followed across services, and it is null when the publisher did
 * not have one. Adding it did not change the version, because an optional field is a compatible change.
 */
public record EventEnvelope(UUID eventId, String eventType, int version, Instant occurredAt, long aggregateId,
                            String correlationId, JsonNode payload) {
}
