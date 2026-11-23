package com.geopslabs.geops.engagement.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * A consumer started following a business (US13). Topic {@code engagement.events}, key {@code consumerId}, contract
 * {@code platform/messaging/events/BusinessFollowed.v1.json}. Notification keeps the followers of each business to
 * tell them about its new offers.
 */
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
