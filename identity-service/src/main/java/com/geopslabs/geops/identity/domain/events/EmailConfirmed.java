package com.geopslabs.geops.identity.domain.events;

import com.geopslabs.geops.identity.domain.models.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The user opened the confirmation link of its email (US28, Tabla 14). Notification marks its copy of the recipient
 * as confirmed, which the email channel requires (US47). The key in identity.events is the userId, the same as
 * UserRegistered, so both arrive in order.
 */
public record EmailConfirmed(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "EmailConfirmed";
    public static final int VERSION = 1;

    public EmailConfirmed {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static EmailConfirmed of(User user, UUID eventId) {
        return new EmailConfirmed(eventId, user.getEmailConfirmedAt(),
                new Payload(user.getId(), user.getEmail(), user.getEmailConfirmedAt()));
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
        return payload.userId();
    }

    public record Payload(Long userId, String email, Instant confirmedAt) {
    }
}
