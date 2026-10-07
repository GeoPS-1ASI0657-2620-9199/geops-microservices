package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.application.usecases.RecordedLocation;

import java.time.Instant;
import java.time.ZoneOffset;

public record LastKnownLocationResponse(Long consumerId, Double latitude, Double longitude, Integer accuracyMeters,
                                        Integer radiusMeters, Instant capturedAt, boolean fresh) {

    public static LastKnownLocationResponse from(RecordedLocation recorded) {
        var location = recorded.location();
        return new LastKnownLocationResponse(recorded.consumerId(), location.position().latitude(),
                location.position().longitude(), location.accuracyMeters(), location.radiusMeters(),
                location.capturedAt().toInstant(ZoneOffset.UTC), recorded.fresh());
    }
}
