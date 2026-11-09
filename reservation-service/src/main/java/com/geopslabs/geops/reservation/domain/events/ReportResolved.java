package com.geopslabs.geops.reservation.domain.events;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportClosedBy;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.domain.models.Reservation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * The report was closed, by the business or by the deadline. {@code previousStatus} tells Catalog whether the report
 * still counted as open when it closed.
 */
public record ReportResolved(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "ReportResolved";
    public static final int VERSION = 1;

    public ReportResolved {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static ReportResolved of(NonComplianceReport closed, ReportStatus previousStatus, ReportClosedBy closedBy,
                                    Reservation reservation, UUID eventId) {
        var resolvedAt = closed.getResolvedAt().toInstant(ZoneOffset.UTC);
        return new ReportResolved(eventId, resolvedAt, new Payload(closed.getId(), closed.getReservationId(),
                reservation.getCode().value(), closed.getConsumerId(), closed.getBusinessId(),
                reservation.getOfferTitle(), closed.getResolution().name(), closedBy.name(), previousStatus.name(),
                resolvedAt));
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
                          Long businessId, String offerTitle, String resolution, String closedBy,
                          String previousStatus, Instant resolvedAt) {
    }
}
