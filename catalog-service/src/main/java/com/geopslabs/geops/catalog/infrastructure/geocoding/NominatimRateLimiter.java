package com.geopslabs.geops.catalog.infrastructure.geocoding;

import java.time.Duration;

/**
 * At most one request per interval to the public Nominatim (E2-15). A caller that arrives too early waits for its
 * turn; requests are spaced, never dropped.
 */
public class NominatimRateLimiter {
    private final long intervalNanos;
    private long nextAllowedAt = Long.MIN_VALUE;

    public NominatimRateLimiter(Duration minInterval) {
        this.intervalNanos = minInterval.toNanos();
    }

    public synchronized void acquire() throws InterruptedException {
        var now = System.nanoTime();
        if (nextAllowedAt != Long.MIN_VALUE && now < nextAllowedAt) {
            var waitNanos = nextAllowedAt - now;
            Thread.sleep(waitNanos / 1_000_000, (int) (waitNanos % 1_000_000));
            now = System.nanoTime();
        }
        nextAllowedAt = now + intervalNanos;
    }
}
