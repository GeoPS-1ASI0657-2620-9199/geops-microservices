package com.geopslabs.geops.identity.domain.models;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * One attempt to verify a business against the SUNAT registry and the geocoder, as stored in ruc_verifications.
 * The checks run in the order of BRD-09, RF-01 and stop at the first one that fails, so the address is geocoded only
 * for an active and habido taxpayer. The status and condition read from the registry are kept with the attempt.
 */
public record RucVerification(Long id, Long businessProfileId, Ruc ruc, VerificationResult result,
                              VerificationReason reason, String taxpayerStatus, String taxpayerCondition,
                              Instant checkedAt) {

    public RucVerification {
        Objects.requireNonNull(businessProfileId);
        Objects.requireNonNull(ruc);
        Objects.requireNonNull(result);
        Objects.requireNonNull(checkedAt);
        if ((result == VerificationResult.APPROVED) != (reason == null)) {
            throw new IllegalArgumentException("Only an approved verification has no reason");
        }
    }

    public static RucVerification evaluate(Long businessProfileId, Ruc ruc, Optional<Taxpayer> taxpayer,
                                           Supplier<GeocodingResult> geocodeAddress, Instant checkedAt) {
        if (taxpayer.isEmpty()) {
            return new RucVerification(null, businessProfileId, ruc, VerificationResult.REJECTED,
                    VerificationReason.RUC_NOT_FOUND, null, null, checkedAt);
        }
        var found = taxpayer.get();
        if (!found.isActive()) {
            return of(businessProfileId, ruc, found, VerificationResult.REJECTED,
                    VerificationReason.TAXPAYER_NOT_ACTIVE, checkedAt);
        }
        if (!found.isHabido()) {
            return of(businessProfileId, ruc, found, VerificationResult.REJECTED,
                    VerificationReason.TAXPAYER_NOT_HABIDO, checkedAt);
        }
        return switch (geocodeAddress.get().outcome()) {
            case FOUND -> of(businessProfileId, ruc, found, VerificationResult.APPROVED, null, checkedAt);
            case NOT_FOUND -> of(businessProfileId, ruc, found, VerificationResult.REJECTED,
                    VerificationReason.ADDRESS_NOT_GEOCODABLE, checkedAt);
            case UNAVAILABLE -> of(businessProfileId, ruc, found, VerificationResult.PENDING,
                    VerificationReason.GEOCODER_UNAVAILABLE, checkedAt);
        };
    }

    public RucVerification withId(Long newId) {
        return new RucVerification(newId, businessProfileId, ruc, result, reason, taxpayerStatus, taxpayerCondition,
                checkedAt);
    }

    public boolean isApproved() {
        return result == VerificationResult.APPROVED;
    }

    private static RucVerification of(Long businessProfileId, Ruc ruc, Taxpayer taxpayer, VerificationResult result,
                                      VerificationReason reason, Instant checkedAt) {
        return new RucVerification(null, businessProfileId, ruc, result, reason, taxpayer.status(),
                taxpayer.condition(), checkedAt);
    }
}
