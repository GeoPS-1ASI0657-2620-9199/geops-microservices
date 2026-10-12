package com.geopslabs.geops.catalog.acceptance;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * The clock of the acceptance tests: fixed, but a scenario can move it ("the date moves to ..."). Reset before every
 * scenario.
 */
public class MutableClock extends Clock {
    private final Instant start;
    private final ZoneId zone;
    private volatile Instant now;

    public MutableClock(Instant start, ZoneId zone) {
        this.start = start;
        this.zone = zone;
        this.now = start;
    }

    public void reset() {
        now = start;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId otherZone) {
        var shifted = new MutableClock(start, otherZone);
        shifted.now = now;
        return shifted;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
