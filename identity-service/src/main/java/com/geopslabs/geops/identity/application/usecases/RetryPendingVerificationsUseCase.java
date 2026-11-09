package com.geopslabs.geops.identity.application.usecases;

import java.util.List;

/**
 * Verifications left PENDING because the geocoder did not answer are tried again, one transaction per business.
 */
public interface RetryPendingVerificationsUseCase {
    List<Long> pendingBusinessIds();

    VerificationOutcome retry(Long businessId);
}
