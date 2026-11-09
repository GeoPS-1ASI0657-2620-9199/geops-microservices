package com.geopslabs.geops.catalog.application.usecases;

import com.geopslabs.geops.catalog.domain.models.ComplianceChange;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * What Catalog reads of a Reservation event that changes the compliance of a business (BRD-13, RF-03):
 * ReservationRedeemed, ReportOpened, ReportAnswered or ReportResolved, already turned into its change.
 */
public record ComplianceEvent(UUID eventId, String eventType, Instant occurredAt, Long businessId,
                              ComplianceChange change) {
    public ComplianceEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(businessId);
        Objects.requireNonNull(change);
    }
}
