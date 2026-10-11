package com.geopslabs.geops.identity.domain.events;

import com.geopslabs.geops.identity.domain.models.BusinessProfile;
import com.geopslabs.geops.identity.domain.models.RucVerification;
import com.geopslabs.geops.identity.domain.models.VerificationReason;
import com.geopslabs.geops.identity.domain.models.VerificationResult;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The business gained, kept or lost the verified seal (BRD-09, 7.3). Catalog recalculates its seal from verified.
 * The key in identity.events is the businessId.
 */
public record BusinessVerified(UUID eventId, Instant occurredAt, Payload payload) implements DomainEvent {
    public static final String EVENT_TYPE = "BusinessVerified";
    public static final int VERSION = 1;

    public BusinessVerified {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(payload);
    }

    public static BusinessVerified of(BusinessProfile profile, RucVerification verification, UUID eventId) {
        return new BusinessVerified(eventId, verification.checkedAt(), new Payload(profile.getId(),
                profile.getBusinessName(), verification.isApproved(), verification.result(), verification.reason(),
                verification.checkedAt()));
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
        return payload.businessId();
    }

    public record Payload(Long businessId, String businessName, boolean verified, VerificationResult result,
                          VerificationReason reason, Instant verifiedAt) {
    }
}
