package com.geopslabs.geops.reservation.domain.events;

import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * The business validated the code of a reservation at the counter (US41). Topic {@code reservation.events}, key
 * {@code reservationId}, contract {@code platform/messaging/events/ReservationRedeemed.v1.json}. Catalog counts the
 * redemption in the compliance of the business (US44) and Engagement lets the consumer review it (US49).
 * {@code occurredAt} and {@code redeemedAt} are the same instant, in UTC.
 */
public record ReservationRedeemed(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "ReservationRedeemed";
    public static final int VERSION = 1;

    public ReservationRedeemed {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static ReservationRedeemed of(Reservation redeemed, UUID eventId) {
        if (redeemed.getRedeemedAt() == null) {
            throw new IllegalArgumentException("Only a redeemed reservation produces ReservationRedeemed");
        }
        var redeemedAt = redeemed.getRedeemedAt().toInstant(ZoneOffset.UTC);
        return new ReservationRedeemed(eventId, redeemedAt, new Payload(redeemed.getId(), redeemed.getConsumerId(),
                redeemed.getBusinessId(), redeemed.getOfferId(), redeemedAt));
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

    public record Payload(Long reservationId, Long consumerId, Long businessId, Long offerId, Instant redeemedAt) {
    }
}
