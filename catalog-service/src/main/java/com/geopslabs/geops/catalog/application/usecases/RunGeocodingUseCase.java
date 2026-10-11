package com.geopslabs.geops.catalog.application.usecases;

/**
 * US07: convert the locations stored as text and report how many were converted and how many need review.
 */
public interface RunGeocodingUseCase {
    GeocodingRunReport run();

    GeocodingSummary summary();
}
