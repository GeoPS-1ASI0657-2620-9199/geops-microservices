package com.geopslabs.geops.reservation.acceptance;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The clock every bean receives in the acceptance suite. A scenario moves it with "the current time is".
 */
public class MutableClock extends Clock {
    private volatile Instant now;

    public MutableClock(Instant now) {
        this.now = now;
    }

    public void setInstant(Instant instant) {
        this.now = instant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
