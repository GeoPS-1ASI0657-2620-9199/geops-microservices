package com.geopslabs.geops.catalog.infrastructure.scheduling;

import com.geopslabs.geops.catalog.application.services.GeocodingRunScheduler;
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
    private final GeocodingRunScheduler geocoding;

    public CatalogJobs(OfferExpirationScheduler expiration, GeocodingRunScheduler geocoding) {
        this.expiration = expiration;
        this.geocoding = geocoding;
    }

    @Scheduled(initialDelayString = "${geops.expiration.initial-delay:1m}",
            fixedDelayString = "${geops.expiration.delay:5m}")
    public void expireOffers() {
        expiration.expireOffers();
    }

    @Scheduled(initialDelayString = "${geops.geocoding.initial-delay:2m}",
            fixedDelayString = "${geops.geocoding.delay:30m}")
    public void geocodePendingOffers() {
        geocoding.runIfPending();
    }
}
