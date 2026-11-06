package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.application.usecases.*;
import com.geopslabs.geops.engagement.domain.models.*;
import com.geopslabs.geops.engagement.domain.ports.SnapshotProjectionPort;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

public class SnapshotProjectorService implements ProjectCatalogEventsUseCase, ProjectRedemptionUseCase {
    private final SnapshotProjectionPort projections;
    public SnapshotProjectorService(SnapshotProjectionPort projections) { this.projections = projections; }

    @Override
    @Transactional
    public void offerPublished(BusinessSnapshot business, OfferSnapshot offer, Instant occurredAt) {
        projections.publish(business, offer, occurredAt);
    }

    @Override
    @Transactional
    public void campaignPaused(Collection<Long> offerIds, Instant occurredAt) {
        projections.changeStatus(offerIds, OfferSnapshot.PAUSED, occurredAt);
    }

    @Override
    @Transactional
    public void offerExpired(Long offerId, Instant occurredAt) {
        projections.changeStatus(List.of(offerId), OfferSnapshot.EXPIRED, occurredAt);
    }

    @Override
    @Transactional
    public void reservationRedeemed(RedeemedReservation reservation) { projections.redeem(reservation); }
}
