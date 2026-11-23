package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.RecordOfferInteractionsUseCase;
import com.geopslabs.geops.catalog.domain.models.OfferInteractions;
import com.geopslabs.geops.catalog.domain.ports.CampaignMetricsRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts impressions and clicks in memory and writes them every few seconds in one batch (US11). The search adds one
 * to an atomic counter per offer of the page and goes on: no query, no Redis call, no lock on its path. The cost is
 * that a crash loses at most the counts of the last interval, which is acceptable for an approximate counter, and
 * that the panel sees them up to one interval late. Each instance flushes its own counts and the database adds them,
 * so several instances need no coordination.
 */
public class OfferInteractionCounter implements RecordOfferInteractionsUseCase {
    /** A bound for the pending keys (offer and day) so an unexpected burst cannot fill the heap. */
    static final int MAX_PENDING_KEYS = 100_000;
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferInteractionCounter.class);

    private final Map<Key, Counters> pending = new ConcurrentHashMap<>();
    private final CampaignMetricsRepositoryPort metrics;
    private final Clock clock;

    public OfferInteractionCounter(CampaignMetricsRepositoryPort metrics, Clock clock) {
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public void recordImpressions(Collection<Long> offerIds) {
        if (offerIds.isEmpty()) {
            return;
        }
        var today = today();
        for (var offerId : offerIds) {
            var counters = countersOf(offerId, today);
            if (counters != null) {
                counters.impressions.incrementAndGet();
            }
        }
    }

    @Override
    public void recordClick(Long offerId) {
        var counters = countersOf(offerId, today());
        if (counters != null) {
            counters.clicks.incrementAndGet();
        }
    }

    /**
     * Takes what was counted since the last flush and adds it to campaign_daily_metrics. If the write fails the counts
     * go back to memory and are tried again on the next flush. Returns the number of offers written.
     */
    public synchronized int flush() {
        var batch = new ArrayList<OfferInteractions>();
        pending.forEach((key, counters) -> {
            var impressions = counters.impressions.getAndSet(0);
            var clicks = counters.clicks.getAndSet(0);
            if (impressions > 0 || clicks > 0) {
                batch.add(new OfferInteractions(key.offerId(), key.date(), impressions, clicks));
            }
        });
        var today = today();
        pending.entrySet().removeIf(entry -> entry.getKey().date().isBefore(today) && entry.getValue().isEmpty());
        if (batch.isEmpty()) {
            return 0;
        }
        try {
            metrics.addInteractions(batch);
        } catch (RuntimeException exception) {
            putBack(batch);
            LOGGER.warn("campaign.metrics.flush-failed offers={} reason={}", batch.size(),
                    exception.getClass().getSimpleName());
            return 0;
        }
        LOGGER.info("campaign.metrics.flushed offers={} impressions={} clicks={}", batch.size(),
                batch.stream().mapToLong(OfferInteractions::impressions).sum(),
                batch.stream().mapToLong(OfferInteractions::clicks).sum());
        return batch.size();
    }

    int pendingKeys() {
        return pending.size();
    }

    private void putBack(List<OfferInteractions> batch) {
        for (var interactions : batch) {
            var counters = pending.computeIfAbsent(new Key(interactions.offerId(), interactions.date()),
                    key -> new Counters());
            counters.impressions.addAndGet(interactions.impressions());
            counters.clicks.addAndGet(interactions.clicks());
        }
    }

    private Counters countersOf(Long offerId, LocalDate date) {
        if (offerId == null) {
            return null;
        }
        var key = new Key(offerId, date);
        var counters = pending.get(key);
        if (counters != null) {
            return counters;
        }
        if (pending.size() >= MAX_PENDING_KEYS) {
            LOGGER.warn("campaign.metrics.dropped offerId={} reason=too-many-pending-keys", offerId);
            return null;
        }
        return pending.computeIfAbsent(key, ignored -> new Counters());
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(OfferQueryService.OFFER_ZONE));
    }

    private record Key(Long offerId, LocalDate date) {
    }

    private static final class Counters {
        private final AtomicLong impressions = new AtomicLong();
        private final AtomicLong clicks = new AtomicLong();

        private boolean isEmpty() {
            return impressions.get() == 0 && clicks.get() == 0;
        }
    }
}
