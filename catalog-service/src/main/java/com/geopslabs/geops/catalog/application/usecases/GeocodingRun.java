package com.geopslabs.geops.catalog.application.usecases;

import java.time.Instant;
import java.util.UUID;

/**
 * A geocoding run as the administrator follows it: RUNNING while it goes on, then FINISHED with its report, SKIPPED
 * if another replica held the lock, or FAILED.
 */
public record GeocodingRun(UUID runId, Status status, Instant startedAt, Instant finishedAt,
                           GeocodingRunReport report) {

    public enum Status {
        RUNNING,
        FINISHED,
        SKIPPED,
        FAILED
    }

    public static GeocodingRun running(UUID runId, Instant startedAt) {
        return new GeocodingRun(runId, Status.RUNNING, startedAt, null, null);
    }

    public GeocodingRun finished(GeocodingRunReport runReport) {
        return new GeocodingRun(runId, Status.FINISHED, startedAt, runReport.finishedAt(), runReport);
    }

    public GeocodingRun skipped(Instant at) {
        return new GeocodingRun(runId, Status.SKIPPED, startedAt, at, null);
    }

    public GeocodingRun failed(Instant at) {
        return new GeocodingRun(runId, Status.FAILED, startedAt, at, null);
    }
}
