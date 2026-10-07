package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.RedeemedReservation;

import java.util.Optional;

public interface RedeemedReservationRepositoryPort {
    Optional<RedeemedReservation> findUnreviewed(Long consumerId, Long businessId);

    boolean existsFor(Long consumerId, Long businessId);

    boolean saveIfAbsent(RedeemedReservation redemption);
}
