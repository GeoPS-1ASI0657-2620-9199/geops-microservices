package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.GeoCell;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.ports.CellCachePort;
import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CellInvalidatorTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
    private static final GeoPoint MIRAFLORES = new GeoPoint(-12.1211, -77.0297);
    private static final GeoPoint CERCADO = new GeoPoint(-12.0464, -77.0306);

    private CellCachePort cache;
    private OfferRepositoryPort offers;
    private CellInvalidator invalidator;

    @BeforeEach
    void setUp() {
        cache = mock(CellCachePort.class);
        offers = mock(OfferRepositoryPort.class);
        invalidator = new CellInvalidator(cache, offers, CLOCK);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void outsideATransactionTheCellsAreDeletedAtOnce() {
        invalidator.evictCellsOf(Arrays.asList(MIRAFLORES, CERCADO, null));

        verify(cache).evict(Set.of(cell(MIRAFLORES), cell(CERCADO)));
    }

    @Test
    void insideATransactionTheCellsAreDeletedOnlyAfterTheCommit() {
        TransactionSynchronizationManager.initSynchronization();

        invalidator.evictCellsOf(List.of(MIRAFLORES));

        verify(cache, never()).evict(any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cache).evict(Set.of(cell(MIRAFLORES)));
    }

    @Test
    void theCellsOfABusinessAreTheOnesOfItsValidOffers() {
        when(offers.findPublishedLocationsOfBusiness(84L, TODAY)).thenReturn(List.of(MIRAFLORES));

        invalidator.evictCellsOfBusiness(84L);

        verify(cache).evict(Set.of(cell(MIRAFLORES)));
    }

    @Test
    void aBusinessWithoutValidOffersDeletesNothing() {
        when(offers.findPublishedLocationsOfBusiness(84L, TODAY)).thenReturn(List.of());

        invalidator.evictCellsOfBusiness(84L);

        verifyNoInteractions(cache);
    }

    @Test
    void theExpirationRoundMarksTheOffersAndDeletesTheirCells() {
        var lock = mock(JobLockPort.class);
        when(lock.tryLock(OfferExpirationScheduler.LOCK)).thenReturn(true);
        when(offers.expirePublishedBefore(TODAY)).thenReturn(List.of(MIRAFLORES));
        var scheduler = new OfferExpirationScheduler(offers, lock, invalidator,
                TransactionOperations.withoutTransaction(), CLOCK);

        assertThat(scheduler.expireOffers()).isEqualTo(1);
        verify(cache).evict(Set.of(cell(MIRAFLORES)));
    }

    @Test
    void anotherReplicaHoldingTheLockSkipsTheRound() {
        var lock = mock(JobLockPort.class);
        when(lock.tryLock(OfferExpirationScheduler.LOCK)).thenReturn(false);
        var scheduler = new OfferExpirationScheduler(offers, lock, invalidator,
                TransactionOperations.withoutTransaction(), CLOCK);

        assertThat(scheduler.expireOffers()).isEqualTo(-1);
        verify(offers, never()).expirePublishedBefore(any());
        verifyNoInteractions(cache);
    }

    private static String cell(GeoPoint point) {
        return GeoCell.containing(point).geohash();
    }
}
