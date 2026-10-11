package com.geopslabs.geops.catalog.infrastructure.scheduling;

import com.geopslabs.geops.catalog.application.services.OfferExpirationScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers of the background jobs. geops.jobs.enabled=false turns them off (acceptance tests run them by hand).
 */
@Component
@ConditionalOnProperty(name = "geops.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class CatalogJobs {
    private final OfferExpirationScheduler expiration;

    public CatalogJobs(OfferExpirationScheduler expiration) {
        this.expiration = expiration;
    }

    @Scheduled(initialDelayString = "${geops.expiration.initial-delay:1m}",
            fixedDelayString = "${geops.expiration.delay:5m}")
    public void expireOffers() {
        expiration.expireOffers();
    }
}
