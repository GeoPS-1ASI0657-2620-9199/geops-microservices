package com.geopslabs.geops.notification.acceptance;

import java.time.*;
public class MutableClock extends Clock {
    private Instant now = AcceptanceTestConfiguration.ACCEPTANCE_NOW;
    public void reset() { now = AcceptanceTestConfiguration.ACCEPTANCE_NOW; }
    public void advance(Duration duration) { now = now.plus(duration); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
    @Override public Instant instant() { return now; }
}
