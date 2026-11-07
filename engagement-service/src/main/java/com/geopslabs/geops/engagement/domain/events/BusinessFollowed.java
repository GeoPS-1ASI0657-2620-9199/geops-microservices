package com.geopslabs.geops.engagement.domain.events;

import java.time.Instant;
import java.util.UUID;

public record BusinessFollowed(UUID eventId, Instant occurredAt, Long aggregateId, Payload payload) implements DomainEvent {
    private static final int VERSION = 1;

    public static BusinessFollowed of(Long consumerId, Long businessId, Instant now) {
        return new BusinessFollowed(UUID.randomUUID(), now, consumerId, new Payload(consumerId, businessId, now));
    }

    @Override
    public String eventType() { return "BusinessFollowed"; }

    @Override
    public int version() { return VERSION; }

    public record Payload(Long consumerId, Long businessId, Instant followedAt) {}
}
