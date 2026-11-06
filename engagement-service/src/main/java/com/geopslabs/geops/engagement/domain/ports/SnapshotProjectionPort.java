package com.geopslabs.geops.engagement.domain.ports;

import com.geopslabs.geops.engagement.domain.models.*;
import java.time.Instant;
import java.util.Collection;

public interface SnapshotProjectionPort {
    void publish(BusinessSnapshot business, OfferSnapshot offer, Instant occurredAt);
    void changeStatus(Collection<Long> offerIds, String status, Instant occurredAt);
    void redeem(RedeemedReservation reservation);
}
