package com.geopslabs.geops.identity.domain.events;

import com.geopslabs.geops.identity.domain.models.ConsumerProfile;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The consumer saved a new walking radius (E2-07, Tabla 14). The consumerId is the user id of the token.
 */
public record ConsumerPreferencesChanged(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "ConsumerPreferencesChanged";
    public static final int VERSION = 1;

    public ConsumerPreferencesChanged {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static ConsumerPreferencesChanged of(ConsumerProfile profile, UUID eventId, Instant occurredAt) {
        return new ConsumerPreferencesChanged(eventId, occurredAt,
                new Payload(profile.getUserId(), profile.getSearchRadiusMinutes()));
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public Long aggregateId() {
        return payload.consumerId();
    }

    public record Payload(Long consumerId, int radiusMinutes) {
    }
}
