package com.geopslabs.geops.engagement.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * A consumer stopped following a business (US13). Topic {@code engagement.events}, key {@code consumerId}, contract
 * {@code platform/messaging/events/BusinessUnfollowed.v1.json}. Notification removes the consumer from the followers.
 */
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
