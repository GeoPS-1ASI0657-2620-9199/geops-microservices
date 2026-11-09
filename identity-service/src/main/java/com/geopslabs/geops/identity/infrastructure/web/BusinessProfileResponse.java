package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.BusinessProfileView;
import com.geopslabs.geops.identity.domain.models.VerificationReason;
import com.geopslabs.geops.identity.domain.models.VerificationResult;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;

import java.time.Instant;

/**
 * The owner's panel: profile data, verification status and the last attempt (null before the first one).
 */
public record BusinessProfileResponse(Long id, String businessName, String businessType, String ruc,
                                      String address, VerificationStatus verificationStatus,
                                      LastVerification lastVerification) {

    static BusinessProfileResponse from(BusinessProfileView view) {
        var profile = view.profile();
        var last = view.latestVerification()
                .map(verification -> new LastVerification(verification.result(), verification.reason(),
                        VerificationMessages.of(verification, profile.getAddress(),
                                profile.getVerificationStatus()), verification.checkedAt()))
                .orElse(null);
        return new BusinessProfileResponse(profile.getId(), profile.getBusinessName(), profile.getBusinessType(),
                profile.getRuc().number(), profile.getAddress(), profile.getVerificationStatus(), last);
    }

    public record LastVerification(VerificationResult result, VerificationReason reason, String message,
                                   Instant checkedAt) {
    }
}
