package com.geopslabs.geops.reservation.domain.events;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/** The business answered before the deadline (US43). Notification tells the consumer; Catalog stops counting it as open. */
public record ReportAnswered(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "ReportAnswered";
    public static final int VERSION = 1;

    public ReportAnswered {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static ReportAnswered of(NonComplianceReport answered, Reservation reservation, UUID eventId) {
        var answeredAt = answered.getAnswer().respondedAt().toInstant(ZoneOffset.UTC);
        return new ReportAnswered(eventId, answeredAt, new Payload(answered.getId(), answered.getReservationId(),
                reservation.getCode().value(), answered.getConsumerId(), answered.getBusinessId(),
                reservation.getOfferTitle(), answeredAt));
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

    public record Payload(Long reportId, Long reservationId, String reservationCode, Long consumerId,
                          Long businessId, String offerTitle, Instant answeredAt) {
    }
}
