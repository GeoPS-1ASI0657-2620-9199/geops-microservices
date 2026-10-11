package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.GeocodingRun;
import com.geopslabs.geops.catalog.application.usecases.GeocodingRunReport;
import com.geopslabs.geops.catalog.domain.models.GeoCell;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.GeocodingResult;
import com.geopslabs.geops.catalog.domain.models.GeocodingStatus;
import com.geopslabs.geops.catalog.domain.models.Money;
import com.geopslabs.geops.catalog.domain.models.Offer;
import com.geopslabs.geops.catalog.domain.ports.CellCachePort;
import com.geopslabs.geops.catalog.domain.ports.GeocodingPort;
import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeocodingRunServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final GeoPoint CERCADO = new GeoPoint(-12.0464, -77.0306);
    private static final String FOUND_ADDRESS = "Jr. de la Unión 300, Cercado de Lima";
    private static final String UNKNOWN_ADDRESS = "Calle que no existe 999";

    private OfferRepositoryPort offers;
    private GeocodingPort geocoding;
    private JobLockPort lock;
    private CellCachePort cache;
    private GeocodingRunService runs;

    @BeforeEach
    void setUp() {
        offers = mock(OfferRepositoryPort.class);
        geocoding = mock(GeocodingPort.class);
        lock = new FreeLock();
        cache = mock(CellCachePort.class);
        runs = new GeocodingRunService(offers, geocoding, lock, new CellInvalidator(cache, offers, CLOCK),
                TransactionOperations.withoutTransaction(), CLOCK, Runnable::run);
        when(offers.markGeocoded(anyLong(), any())).thenReturn(true);
        when(offers.markForManualReview(anyLong())).thenReturn(true);
        when(offers.countPublicSourceByGeocodingStatus()).thenReturn(Map.of());
    }

    @Test
    void eachOfferEndsWithCoordinatesOrInManualReview() {
        when(offers.findPendingGeocoding()).thenReturn(List.of(pending(1L, FOUND_ADDRESS), pending(2L, UNKNOWN_ADDRESS)));
        when(geocoding.geocode(FOUND_ADDRESS)).thenReturn(GeocodingResult.found(CERCADO));
        when(geocoding.geocode(UNKNOWN_ADDRESS)).thenReturn(GeocodingResult.notFound());

        var report = runAndReport();

        assertThat(report.processed()).isEqualTo(2);
        assertThat(report.geocoded()).isEqualTo(1);
        assertThat(report.manualReview()).isEqualTo(1);
        assertThat(report.stoppedByProvider()).isFalse();
        verify(offers).markGeocoded(1L, CERCADO);
        verify(offers).markForManualReview(2L);
        verify(cache).evict(Set.of(GeoCell.containing(CERCADO).geohash()));
    }

    @Test
    void theFirstUnavailableAnswerStopsTheRunAndLeavesTheRestPending() {
        when(offers.findPendingGeocoding()).thenReturn(List.of(pending(1L, FOUND_ADDRESS), pending(2L, UNKNOWN_ADDRESS)));
        when(geocoding.geocode(FOUND_ADDRESS)).thenReturn(GeocodingResult.unavailable());
        when(offers.countPublicSourceByGeocodingStatus()).thenReturn(Map.of(GeocodingStatus.PENDING, 2L));

        var report = runAndReport();

        assertThat(report.stoppedByProvider()).isTrue();
        assertThat(report.processed()).isZero();
        assertThat(report.stillPending()).isEqualTo(2);
        verify(geocoding, never()).geocode(UNKNOWN_ADDRESS);
        verify(offers, never()).markGeocoded(anyLong(), any());
        verify(offers, never()).markForManualReview(anyLong());
    }

    @Test
    void anOfferThatStoppedBeingPendingIsNotCounted() {
        when(offers.findPendingGeocoding()).thenReturn(List.of(pending(1L, FOUND_ADDRESS)));
        when(geocoding.geocode(FOUND_ADDRESS)).thenReturn(GeocodingResult.found(CERCADO));
        when(offers.markGeocoded(eq(1L), any())).thenReturn(false);

        assertThat(runAndReport().processed()).isZero();
        verify(cache, never()).evict(any());
    }

    @Test
    void theCallOnlyStartsTheRunAndTheSummaryFollowsIt() {
        var background = new ArrayList<Runnable>();
        var async = new GeocodingRunService(offers, geocoding, lock, mock(CellInvalidator.class),
                TransactionOperations.withoutTransaction(), CLOCK, background::add);
        when(offers.findPendingGeocoding()).thenReturn(List.of());

        var started = async.start();

        assertThat(started.status()).isEqualTo(GeocodingRun.Status.RUNNING);
        assertThat(async.summary().lastRun().runId()).isEqualTo(started.runId());
        verify(offers, never()).findPendingGeocoding();
        background.forEach(Runnable::run);
        assertThat(async.summary().lastRun().status()).isEqualTo(GeocodingRun.Status.FINISHED);
        assertThat(async.summary().lastRun().report()).isNotNull();
    }

    @Test
    void aSecondRunWhileTheFirstGoesOnIsRejected() {
        var background = new ArrayList<Runnable>();
        var async = new GeocodingRunService(offers, geocoding, lock, mock(CellInvalidator.class),
                TransactionOperations.withoutTransaction(), CLOCK, background::add);
        async.start();

        assertThatThrownBy(async::start).isInstanceOf(GeocodingRunInProgressException.class)
                .extracting("code").isEqualTo("GEOCODING_RUN_IN_PROGRESS");
        background.forEach(Runnable::run);
        assertThat(async.start().status()).isEqualTo(GeocodingRun.Status.RUNNING);
    }

    @Test
    void aRunWhileAnotherReplicaHoldsTheLockEndsSkipped() {
        var busy = new GeocodingRunService(offers, geocoding, new BusyLock(), mock(CellInvalidator.class),
                TransactionOperations.withoutTransaction(), CLOCK, Runnable::run);

        busy.start();

        assertThat(busy.summary().lastRun().status()).isEqualTo(GeocodingRun.Status.SKIPPED);
        assertThat(busy.runNow()).isEmpty();
    }

    @Test
    void aRunThatFailsIsReportedAndFreesTheNextOne() {
        when(offers.findPendingGeocoding()).thenThrow(new IllegalStateException("database down"));

        runs.start();

        assertThat(runs.summary().lastRun().status()).isEqualTo(GeocodingRun.Status.FAILED);
        assertThat(runs.start()).isNotNull();
    }

    @Test
    void theSchedulerRunsOnlyWhenOffersWait() {
        var scheduler = new GeocodingRunScheduler(runs);

        scheduler.runIfPending();
        verify(offers, never()).findPendingGeocoding();

        when(offers.countPublicSourceByGeocodingStatus()).thenReturn(Map.of(GeocodingStatus.PENDING, 1L));
        when(offers.findPendingGeocoding()).thenReturn(List.of());
        scheduler.runIfPending();
        verify(offers).findPendingGeocoding();
    }

    @Test
    void theSummaryCountsByStatus() {
        when(offers.countPublicSourceByGeocodingStatus()).thenReturn(Map.of(GeocodingStatus.GEOCODED, 140L,
                GeocodingStatus.MANUAL_REVIEW, 6L));

        var summary = runs.summary();

        assertThat(summary.geocoded()).isEqualTo(140);
        assertThat(summary.manualReview()).isEqualTo(6);
        assertThat(summary.pending()).isZero();
    }

    private GeocodingRunReport runAndReport() {
        runs.start();
        return runs.summary().lastRun().report();
    }

    private static Offer pending(Long id, String address) {
        var offer = Offer.fromPublicSource("Oferta " + id, "Entrada libre", Money.soles(BigDecimal.ZERO),
                LocalDate.of(2026, 10, 30), "Librería y útiles", address, "Municipalidad de Lima", null);
        return new Offer(id, null, null, offer.getTitle(), offer.getConditions(), offer.getPrice(),
                offer.getValidTo(), offer.getCategory(), offer.getGeocodingStatus(), offer.getAddress(), null,
                offer.getSource(), offer.getSourceName(), offer.getStatus(), null);
    }

    private static final class FreeLock implements JobLockPort {
        @Override
        public boolean tryLock(String jobName) {
            return true;
        }

        @Override
        public <T> Optional<T> runExclusively(String jobName, Supplier<T> job) {
            return Optional.ofNullable(job.get());
        }
    }

    private static final class BusyLock implements JobLockPort {
        @Override
        public boolean tryLock(String jobName) {
            return false;
        }

        @Override
        public <T> Optional<T> runExclusively(String jobName, Supplier<T> job) {
            return Optional.empty();
        }
    }
}
