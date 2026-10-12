package com.geopslabs.geops.catalog.application.usecases;

/**
 * US07: convert the locations stored as text and report how many were converted and how many need review. The run
 * goes on in the background; its report is the lastRun of the summary.
 */
public interface RunGeocodingUseCase {
    GeocodingRun start();

    GeocodingSummary summary();
}
