package com.geopslabs.geops.identity.infrastructure.sunat;

import java.time.Duration;

/**
 * Outcome of one load: rows stored for Lima and Callao, rows of other districts left out, malformed lines discarded
 * and how long it took. A skipped load did nothing because another one held the lock.
 */
public record SunatLoadReport(long rows, long filtered, long malformed, Duration duration, boolean wasSkipped) {

    static SunatLoadReport skipped() {
        return new SunatLoadReport(0, 0, 0, Duration.ZERO, true);
    }
}
