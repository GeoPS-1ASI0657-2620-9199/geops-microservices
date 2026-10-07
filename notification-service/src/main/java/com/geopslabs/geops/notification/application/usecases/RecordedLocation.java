package com.geopslabs.geops.notification.application.usecases;

import com.geopslabs.geops.notification.domain.models.LastKnownLocation;

public record RecordedLocation(Long consumerId, LastKnownLocation location, boolean fresh) {
}
