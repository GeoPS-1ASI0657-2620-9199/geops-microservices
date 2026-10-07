package com.geopslabs.geops.notification.domain.models;

import java.time.Duration;
import java.time.LocalDateTime;

public record LastKnownLocation(GeoPoint position, Integer accuracyMeters, Integer radiusMeters,
                                LocalDateTime capturedAt) {
    public static final Duration FRESHNESS = Duration.ofMinutes(60);

    public boolean isFresh(LocalDateTime now) {
        return Duration.between(capturedAt, now).compareTo(FRESHNESS) < 0;
    }

    public boolean isNewerThan(LastKnownLocation other) {
        return capturedAt.isAfter(other.capturedAt());
    }
}
