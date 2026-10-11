package com.geopslabs.geops.catalog.application.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * What Catalog reads of Identity's BusinessVerified (BRD-09, 7.3; envelope of E2-02).
 */
public record BusinessVerifiedEvent(UUID eventId, Instant occurredAt, Long businessId, String businessName,
                                    boolean verified) {
    public static final String EVENT_TYPE = "BusinessVerified";

    public BusinessVerifiedEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(businessId);
    }
}
