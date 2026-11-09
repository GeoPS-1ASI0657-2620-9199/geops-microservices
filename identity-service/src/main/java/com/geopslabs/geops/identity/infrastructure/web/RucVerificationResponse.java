package com.geopslabs.geops.identity.infrastructure.web;

import com.geopslabs.geops.identity.application.usecases.VerificationOutcome;
import com.geopslabs.geops.identity.domain.models.VerificationReason;
import com.geopslabs.geops.identity.domain.models.VerificationResult;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;

import java.time.Instant;

/**
 * Contract of BRD-09, 7.1, plus the checked address and a message with what to do next (QAS13).
 */
public record RucVerificationResponse(Long verificationId, VerificationResult result, VerificationReason reason,
                                      String message, String address, String taxpayerStatus,
                                      String taxpayerCondition, VerificationStatus verificationStatus,
                                      Instant checkedAt) {

    static RucVerificationResponse from(VerificationOutcome outcome) {
        var verification = outcome.verification();
        return new RucVerificationResponse(verification.id(), verification.result(), verification.reason(),
                VerificationMessages.of(verification, outcome.checkedAddress(), outcome.verificationStatus()), outcome.checkedAddress(),
                verification.taxpayerStatus(), verification.taxpayerCondition(), outcome.verificationStatus(),
                verification.checkedAt());
    }
}
