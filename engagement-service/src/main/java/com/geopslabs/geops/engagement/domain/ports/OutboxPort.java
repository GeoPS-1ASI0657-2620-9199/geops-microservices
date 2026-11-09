package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.events.DomainEvent;

/**
 * Stores an event in the outbox inside the caller's transaction; the relay publishes it later.
 */
public interface OutboxPort {
    void append(DomainEvent event);
}
