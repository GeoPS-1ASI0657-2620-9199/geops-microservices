package com.geopslabs.geops.reservation.domain.ports;

import com.geopslabs.geops.reservation.domain.events.DomainEvent;

/**
 * Stores an event in the outbox inside the caller's transaction; the relay publishes it later.
 */
public interface OutboxPort {
    void append(DomainEvent event);
}
