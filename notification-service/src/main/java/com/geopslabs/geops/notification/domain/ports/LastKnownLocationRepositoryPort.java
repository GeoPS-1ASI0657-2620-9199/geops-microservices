package com.geopslabs.geops.notification.domain.ports;

import com.geopslabs.geops.notification.domain.models.LastKnownLocation;

import java.util.Optional;

public interface LastKnownLocationRepositoryPort {
    LastKnownLocation saveIfNewer(Long consumerId, LastKnownLocation location);

    Optional<LastKnownLocation> findByConsumerId(Long consumerId);
}
