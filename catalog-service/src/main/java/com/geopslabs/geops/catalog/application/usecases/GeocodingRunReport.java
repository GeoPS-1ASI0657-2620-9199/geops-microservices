package com.geopslabs.geops.catalog.application.usecases;

import java.time.Instant;

/**
 * BRD-08, 7.5: processed = geocoded + manualReview; stillPending counts the offers left for the next run.
 */
public record GeocodingRunReport(int processed, int geocoded, int manualReview, long stillPending,
                                 boolean stoppedByProvider, Instant startedAt, Instant finishedAt) {
}
