package com.geopslabs.geops.identity.domain.events;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.User;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A consumer or a business owner signed up (BRD-11, RF-04). Notification learns the recipient from this event
 * instead of calling Identity. {@code businessId} is the id of the business profile, the same as the claim of the
 * token, and is null for a consumer. {@code confirmationToken} is the token of the confirmation link that Notification
 * sends by email (US28); it is null when the email is already confirmed, and it is valid until
 * {@code confirmationExpiresAt}. Both are optional fields added to version 1, so a tolerant reader is not affected.
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
        return of(user, businessProfile, null, null, eventId, occurredAt);
    }

    public static UserRegistered of(User user, BusinessProfile businessProfile, String confirmationToken,
                                    Instant confirmationExpiresAt, UUID eventId, Instant occurredAt) {
        return new UserRegistered(eventId, occurredAt, new Payload(user.getId(), user.getEmail(),
                user.getRole().name(), businessProfile == null ? null : businessProfile.getId(),
                user.getEmailConfirmedAt() != null, occurredAt, confirmationToken, confirmationExpiresAt));
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
                          Instant registeredAt, String confirmationToken, Instant confirmationExpiresAt) {
    }
}
