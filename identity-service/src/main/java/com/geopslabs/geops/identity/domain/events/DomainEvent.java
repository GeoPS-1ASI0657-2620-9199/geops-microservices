package com.geopslabs.geops.identity.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope shared by every published event (E2-02): {eventId, eventType, version, occurredAt, aggregateId,
 * payload}. Consumers discard a repeated eventId, so a second delivery is harmless.
 */
public interface DomainEvent {
    UUID eventId();

    String eventType();

    int version();

    Instant occurredAt();

    Long aggregateId();

    Object payload();
}
