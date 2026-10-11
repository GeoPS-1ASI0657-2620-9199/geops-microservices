package com.geopslabs.geops.catalog.application.services;

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

/**
 * Conversión a coordenadas (US07). Walks the offers PENDING in id order, asks Nominatim for each address and saves
 * each answer in its own short transaction, never holding a transaction while Nominatim answers. FOUND sets the
 * location and GEOCODED; NOT_FOUND sets MANUAL_REVIEW; the first UNAVAILABLE stops the run and leaves the rest PENDING
 * for the next one. Only PENDING offers are touched, so a second run does not change what was converted. One run at
 * a time across replicas (advisory lock); a second call gets 409 GEOCODING_RUN_IN_PROGRESS.
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

    public GeocodingRunService(OfferRepositoryPort offers, GeocodingPort geocoding, JobLockPort jobLock,
                               CellInvalidator cellInvalidator, TransactionOperations transactions, Clock clock) {
        this.offers = offers;
        this.geocoding = geocoding;
        this.jobLock = jobLock;
        this.cellInvalidator = cellInvalidator;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public GeocodingRunReport run() {
        return jobLock.runExclusively(LOCK, this::convertPending)
                .orElseThrow(GeocodingRunInProgressException::new);
    }

    @Override
    public GeocodingSummary summary() {
        var counts = offers.countPublicSourceByGeocodingStatus();
        return new GeocodingSummary(counts.getOrDefault(GeocodingStatus.GEOCODED, 0L),
                counts.getOrDefault(GeocodingStatus.MANUAL_REVIEW, 0L),
                counts.getOrDefault(GeocodingStatus.PENDING, 0L));
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
