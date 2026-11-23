package com.geopslabs.geops.engagement.application.services;

import com.geopslabs.geops.engagement.domain.models.OfferSnapshot;
import com.geopslabs.geops.engagement.domain.ports.SnapshotProjectionPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SnapshotProjectorServiceTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-22T15:04:05Z");

    private final SnapshotProjectionPort projections = mock(SnapshotProjectionPort.class);
    private final SnapshotProjectorService projector = new SnapshotProjectorService(projections);

    @Test
    void aRemovedOfferChangesTheLocalCopyToRemoved() {
        projector.offerRemoved(1052L, OCCURRED_AT);

        verify(projections).changeStatus(List.of(1052L), OfferSnapshot.REMOVED, OCCURRED_AT);
    }

    @Test
    void anExpiredOfferChangesTheLocalCopyToExpired() {
        projector.offerExpired(1052L, OCCURRED_AT);

        verify(projections).changeStatus(List.of(1052L), OfferSnapshot.EXPIRED, OCCURRED_AT);
    }
}
