package com.geopslabs.geops.engagement.domain.events;

import java.time.Instant;
import java.util.UUID;

public record BusinessUnfollowed(UUID eventId, Instant occurredAt, Long aggregateId, Payload payload) implements DomainEvent {
    private static final int VERSION = 1;

    public static BusinessUnfollowed of(Long consumerId, Long businessId, Instant now) {
        return new BusinessUnfollowed(UUID.randomUUID(), now, consumerId, new Payload(consumerId, businessId, now));
    }

    @Override
    public String eventType() { return "BusinessUnfollowed"; }

    @Override
    public int version() { return VERSION; }

    public record Payload(Long consumerId, Long businessId, Instant unfollowedAt) {}
}
