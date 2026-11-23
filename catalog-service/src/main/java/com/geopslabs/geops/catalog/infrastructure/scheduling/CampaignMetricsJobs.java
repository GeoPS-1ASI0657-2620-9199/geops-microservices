package com.geopslabs.geops.catalog.infrastructure.scheduling;

import com.geopslabs.geops.catalog.application.services.OfferInteractionCounter;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Writes the impressions and clicks counted in memory (US11) every geops.metrics.flush-delay, and once more when the
 * service stops so a normal restart loses nothing. geops.jobs.enabled=false turns it off, as the other jobs.
 */
@Component
@ConditionalOnProperty(name = "geops.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class CampaignMetricsJobs {
    private final OfferInteractionCounter counter;

    public CampaignMetricsJobs(OfferInteractionCounter counter) {
        this.counter = counter;
    }

    @Scheduled(initialDelayString = "${geops.metrics.flush-delay:15s}",
            fixedDelayString = "${geops.metrics.flush-delay:15s}")
    public void flushInteractions() {
        counter.flush();
    }

    @PreDestroy
    public void flushBeforeStopping() {
        counter.flush();
    }
}
