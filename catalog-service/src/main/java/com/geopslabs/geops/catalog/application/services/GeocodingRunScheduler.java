package com.geopslabs.geops.catalog.application.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries the conversion every 30 minutes while offers wait (Informe: "si falla Nominatim, la oferta se guarda con
 * geocoding_status en PENDING y se reintenta"). A run already in progress, here or in another replica, is skipped.
 */
public class GeocodingRunScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeocodingRunScheduler.class);

    private final GeocodingRunService runs;

    public GeocodingRunScheduler(GeocodingRunService runs) {
        this.runs = runs;
    }

    public void runIfPending() {
        if (!runs.hasPending()) {
            return;
        }
        if (runs.runNow().isEmpty()) {
            LOGGER.debug("offers.geocoding.skipped reason=run-in-progress-or-locked");
        }
    }
}
