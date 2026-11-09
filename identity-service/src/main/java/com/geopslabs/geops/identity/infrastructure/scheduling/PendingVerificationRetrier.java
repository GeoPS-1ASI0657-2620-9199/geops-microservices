package com.geopslabs.geops.identity.infrastructure.scheduling;

import com.geopslabs.geops.identity.application.usecases.RetryPendingVerificationsUseCase;
import com.geopslabs.geops.identity.domain.models.VerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every 10 minutes tries again the verifications that stayed PENDING because Nominatim did not answer (BRD-09,
 * RF-01). Each business runs in its own transaction, so one failure does not undo the others. While the circuit
 * breaker is open the retry answers PENDING again at once, without calling Nominatim.
 */
@Component
@ConditionalOnProperty(name = "geops.verification.retry-enabled", havingValue = "true", matchIfMissing = true)
public class PendingVerificationRetrier {
    private static final Logger LOGGER = LoggerFactory.getLogger(PendingVerificationRetrier.class);

    private final RetryPendingVerificationsUseCase retryPendingVerifications;

    public PendingVerificationRetrier(RetryPendingVerificationsUseCase retryPendingVerifications) {
        this.retryPendingVerifications = retryPendingVerifications;
    }

    @Scheduled(initialDelayString = "${geops.verification.retry-delay:PT10M}",
            fixedDelayString = "${geops.verification.retry-delay:PT10M}")
    public void retryPending() {
        var pending = retryPendingVerifications.pendingBusinessIds();
        if (pending.isEmpty()) {
            return;
        }
        var resolved = 0;
        for (var businessId : pending) {
            try {
                var outcome = retryPendingVerifications.retry(businessId);
                if (outcome.verification().result() != VerificationResult.PENDING) {
                    resolved++;
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("business.verification.retry.failed businessId={} reason={}", businessId,
                        exception.getMessage());
            }
        }
        LOGGER.info("business.verification.retry.completed pending={} resolved={}", pending.size(), resolved);
    }
}
