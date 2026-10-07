package com.geopslabs.geops.notification.domain.models.commands;

import java.time.Instant;

public record RecordLocationCommand(Long consumerId, Double latitude, Double longitude, Integer accuracyMeters,
                                    Instant capturedAt) {
}
