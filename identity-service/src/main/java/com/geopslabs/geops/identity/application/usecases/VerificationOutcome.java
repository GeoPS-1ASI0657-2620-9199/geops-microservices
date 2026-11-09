package com.geopslabs.geops.identity.application.usecases;

import com.geopslabs.geops.identity.domain.models.RucVerification;
import com.geopslabs.geops.identity.domain.models.VerificationStatus;

/**
 * The stored attempt, the status the profile ended in and the address that was checked.
 */
public record VerificationOutcome(RucVerification verification, VerificationStatus verificationStatus,
                                  String checkedAddress) {
}
