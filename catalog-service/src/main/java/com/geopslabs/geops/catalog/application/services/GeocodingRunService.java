package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.GeocodingRun;
import com.geopslabs.geops.catalog.application.usecases.GeocodingRunReport;
import com.geopslabs.geops.catalog.application.usecases.GeocodingSummary;
import com.geopslabs.geops.catalog.application.usecases.RunGeocodingUseCase;
import com.geopslabs.geops.catalog.domain.models.GeocodingResult;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.ports.GeocodingPort;
import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Conversión a coordenadas (US07). Walks the offers PENDING in id order, asks Nominatim for each address and saves
 * each answer in its own short transaction, never holding a transaction while Nominatim answers. FOUND sets the
 * location and GEOCODED; NOT_FOUND sets MANUAL_REVIEW; the first UNAVAILABLE stops the run and leaves the rest PENDING
 * for the next one. Only PENDING offers are touched, so a second run does not change what was converted.
 *
 * <p>The administrator's call only starts the run: it answers 202 with the run id and the run goes on in the
 * background (one request per second to Nominatim makes it last minutes). Its progress and its report are read in
 * the summary (lastRun). One run at a time: a second call to this replica gets 409 GEOCODING_RUN_IN_PROGRESS, and a
 * run started while another replica holds the advisory lock ends SKIPPED.</p>
 */
public class GeocodingRunService implements RunGeocodingUseCase {
    static final String LOCK = "catalog.geocoding-run";
    private static final Logger LOGGER = LoggerFactory.getLogger(GeocodingRunService.class);

    private final OfferRepositoryPort offers;
    private final GeocodingPort geocoding;
    private final JobLockPort jobLock;
    private final CellInvalidator cellInvalidator;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final Executor executor;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<GeocodingRun> lastRun = new AtomicReference<>();

    @SuppressWarnings("java:S107")
    public GeocodingRunService(OfferRepositoryPort offers, GeocodingPort geocoding, JobLockPort jobLock,
                               CellInvalidator cellInvalidator, TransactionOperations transactions, Clock clock,
                               Executor executor) {
        this.offers = offers;
        this.geocoding = geocoding;
        this.jobLock = jobLock;
        this.cellInvalidator = cellInvalidator;
        this.transactions = transactions;
        this.clock = clock;
        this.executor = executor;
    }

    @Override
    public GeocodingRun start() {
        if (!running.compareAndSet(false, true)) {
            throw new GeocodingRunInProgressException();
        }
        var run = GeocodingRun.running(UUID.randomUUID(), clock.instant());
        lastRun.set(run);
        try {
            executor.execute(() -> runInBackground(run));
        } catch (RuntimeException exception) {
            running.set(false);
            lastRun.set(run.failed(clock.instant()));
            throw exception;
        }
        LOGGER.info("offers.geocoding.started runId={}", run.runId());
        return run;
    }

    /** The scheduler's run, in its own thread; empty when a run is already going on or another replica has it. */
    public Optional<GeocodingRunReport> runNow() {
        if (!running.compareAndSet(false, true)) {
            return Optional.empty();
        }
        var run = GeocodingRun.running(UUID.randomUUID(), clock.instant());
        lastRun.set(run);
        return Optional.ofNullable(runInBackground(run));
    }

    @Override
    public GeocodingSummary summary() {
        var counts = offers.countPublicSourceByGeocodingStatus();
        return new GeocodingSummary(counts.getOrDefault(GeocodingStatus.GEOCODED, 0L),
                counts.getOrDefault(GeocodingStatus.MANUAL_REVIEW, 0L),
                counts.getOrDefault(GeocodingStatus.PENDING, 0L), lastRun.get());
    }

    private GeocodingRunReport runInBackground(GeocodingRun run) {
        try {
            var report = jobLock.runExclusively(LOCK, this::convertPending);
            if (report.isEmpty()) {
                LOGGER.info("offers.geocoding.skipped runId={} reason=another-replica-holds-the-lock", run.runId());
                lastRun.set(run.skipped(clock.instant()));
                return null;
            }
            lastRun.set(run.finished(report.get()));
            return report.get();
        } catch (RuntimeException exception) {
            LOGGER.error("offers.geocoding.failed runId={} reason={}", run.runId(),
                    exception.getClass().getSimpleName());
            lastRun.set(run.failed(clock.instant()));
            return null;
        } finally {
            running.set(false);
        }
    }

    public boolean hasPending() {
        return offers.countPublicSourceByGeocodingStatus().getOrDefault(GeocodingStatus.PENDING, 0L) > 0;
    }

    private GeocodingRunReport convertPending() {
        var startedAt = clock.instant();
        var geocoded = 0;
        var manualReview = 0;
        var stoppedByProvider = false;
        for (Offer offer : pending()) {
            var result = geocoding.geocode(offer.getAddress());
            if (result.outcome() == GeocodingResult.Outcome.UNAVAILABLE) {
                stoppedByProvider = true;
                break;
            }
            if (apply(offer, result)) {
                if (result.outcome() == GeocodingResult.Outcome.FOUND) {
                    geocoded++;
                } else {
                    manualReview++;
                }
            }
        }
        var stillPending = offers.countPublicSourceByGeocodingStatus().getOrDefault(GeocodingStatus.PENDING, 0L);
        var report = new GeocodingRunReport(geocoded + manualReview, geocoded, manualReview, stillPending,
                stoppedByProvider, startedAt, clock.instant());
        LOGGER.info("offers.geocoding.finished processed={} geocoded={} manualReview={} stillPending={} "
                        + "stoppedByProvider={}", report.processed(), geocoded, manualReview, stillPending,
                stoppedByProvider);
        return report;
    }

    private List<Offer> pending() {
        return offers.findPendingGeocoding();
    }

    /** False when the offer stopped being PENDING meanwhile; nothing is overwritten then. */
    private boolean apply(Offer offer, GeocodingResult result) {
        var changed = transactions.execute(status -> {
            if (result.outcome() == GeocodingResult.Outcome.FOUND) {
                var updated = offers.markGeocoded(offer.getId(), result.point());
                if (updated) {
                    cellInvalidator.evictCellsOf(List.of(result.point()));
                }
                return updated;
            }
            return offers.markForManualReview(offer.getId());
        });
        return Boolean.TRUE.equals(changed);
    }
}
