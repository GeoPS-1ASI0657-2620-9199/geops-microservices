package com.geopslabs.geops.identity.domain.events;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A consumer or a business owner signed up (BRD-11, RF-04). Notification learns the recipient from this event
 * instead of calling Identity. {@code businessId} is the id of the business profile, the same as the claim of the
 * token, and is null for a consumer. {@code emailConfirmed} is false until the confirmation of US28 exists.
 */
public record UserRegistered(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "UserRegistered";
    public static final int VERSION = 1;

    public UserRegistered {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static UserRegistered of(User user, BusinessProfile businessProfile, UUID eventId, Instant occurredAt) {
        return new UserRegistered(eventId, occurredAt, new Payload(user.getId(), user.getEmail(),
                user.getRole().name(), businessProfile == null ? null : businessProfile.getId(),
                user.getEmailConfirmedAt() != null, occurredAt));
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

    public record Payload(Long userId, String email, String role, Long businessId, boolean emailConfirmed,
                          Instant registeredAt) {
    }
}
