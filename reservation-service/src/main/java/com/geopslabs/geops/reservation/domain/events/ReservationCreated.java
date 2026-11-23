package com.geopslabs.geops.reservation.domain.events;

import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/** A new reservation of an offer (US40). Catalog counts it in the metrics of the campaign of the offer (US11). */
public record ReservationCreated(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "ReservationCreated";
    public static final int VERSION = 1;

    public ReservationCreated {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static ReservationCreated of(Reservation created, UUID eventId) {
        if (created.getId() == null) {
            throw new IllegalArgumentException("Only a stored reservation produces ReservationCreated");
        }
        var reservedAt = created.getReservedAt().toInstant(ZoneOffset.UTC);
        return new ReservationCreated(eventId, reservedAt, new Payload(created.getId(), created.getConsumerId(),
                created.getBusinessId(), created.getOfferId(), reservedAt,
                created.getExpiresAt().toInstant(ZoneOffset.UTC)));
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
        return payload.reservationId();
    }

    public record Payload(Long reservationId, Long consumerId, Long businessId, Long offerId, Instant reservedAt,
                          Instant expiresAt) {
    }
}
