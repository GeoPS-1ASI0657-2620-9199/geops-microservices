package com.geopslabs.geops.reservation.domain.events;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/** The consumer reported the offer (Table 14, US42). Catalog counts it as open; Notification tells the business. */
public record ReportOpened(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "ReportOpened";
    public static final int VERSION = 1;

    public ReportOpened {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static ReportOpened of(NonComplianceReport report, Reservation reservation, UUID eventId) {
        var openedAt = report.getOpenedAt().toInstant(ZoneOffset.UTC);
        return new ReportOpened(eventId, openedAt, new Payload(report.getId(), report.getReservationId(),
                reservation.getCode().value(), report.getConsumerId(), report.getBusinessId(),
                reservation.getOfferId(), reservation.getOfferTitle(), report.getReason().value(), openedAt,
                report.getResponseDeadline().toInstant(ZoneOffset.UTC)));
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
                          Long businessId, Long offerId, String offerTitle, String reason, Instant openedAt,
                          Instant responseDeadline) {
    }
}
