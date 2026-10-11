package com.geopslabs.geops.catalog.application.usecases;

/**
 * E2-10: the public source offers counted by geocoding_status, without a table of runs, plus the last run this
 * replica started (null if none since it started).
 */
public record GeocodingSummary(long geocoded, long manualReview, long pending, GeocodingRun lastRun) {
}
