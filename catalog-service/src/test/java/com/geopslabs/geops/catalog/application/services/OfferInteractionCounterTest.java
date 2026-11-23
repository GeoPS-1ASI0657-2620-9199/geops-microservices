package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.models.OfferInteractions;
import com.geopslabs.geops.catalog.domain.ports.CampaignMetricsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OfferInteractionCounterTest {
    /** 23:30 of October 7 in Lima, already October 8 in UTC. */
    private static final Instant LATE_NIGHT_IN_LIMA = Instant.parse("2026-10-08T04:30:00Z");
    private static final LocalDate LIMA_DAY = LocalDate.parse("2026-10-07");

    private CampaignMetricsRepositoryPort metrics;
    private OfferInteractionCounter counter;

    @BeforeEach
    void setUp() {
        metrics = mock(CampaignMetricsRepositoryPort.class);
        counter = new OfferInteractionCounter(metrics, Clock.fixed(LATE_NIGHT_IN_LIMA, ZoneOffset.UTC));
    }

    @Test
    void addsUpImpressionsAndClicksPerOfferAndDayOfLimaAndWritesThemInOneBatch() {
        counter.recordImpressions(List.of(1052L, 1053L));
        counter.recordImpressions(List.of(1052L));
        counter.recordClick(1052L);

        assertThat(counter.flush()).isEqualTo(2);

        assertThat(written()).containsExactlyInAnyOrder(new OfferInteractions(1052L, LIMA_DAY, 2, 1),
                new OfferInteractions(1053L, LIMA_DAY, 1, 0));
    }

    @Test
    void aFlushWithoutNewCountsWritesNothing() {
        counter.recordClick(1052L);
        counter.flush();
        reset(metrics);

        assertThat(counter.flush()).isZero();
        verify(metrics, never()).addInteractions(anyCollection());
    }

    @Test
    void whatCouldNotBeWrittenIsTriedAgainOnTheNextFlush() {
        counter.recordImpressions(List.of(1052L));
        doThrow(new IllegalStateException("database down")).when(metrics).addInteractions(anyCollection());
        assertThat(counter.flush()).isZero();
        reset(metrics);
        counter.recordImpressions(List.of(1052L));

        counter.flush();

        assertThat(written()).containsExactly(new OfferInteractions(1052L, LIMA_DAY, 2, 0));
    }

    @Test
    void theDaysAlreadyWrittenAreForgotten() {
        var clock = new MovableClock(LATE_NIGHT_IN_LIMA);
        var moving = new OfferInteractionCounter(metrics, clock);
        moving.recordImpressions(List.of(1052L));
        clock.now = LATE_NIGHT_IN_LIMA.plusSeconds(3600);

        moving.flush();

        assertThat(moving.pendingKeys()).isZero();
    }

    @Test
    void parallelSearchesLoseNoImpression() throws InterruptedException {
        var executor = Executors.newFixedThreadPool(8);
        for (var search = 0; search < 1000; search++) {
            executor.submit(() -> counter.recordImpressions(List.of(1052L)));
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        counter.flush();

        assertThat(written()).containsExactly(new OfferInteractions(1052L, LIMA_DAY, 1000, 0));
    }

    @SuppressWarnings("unchecked")
    private List<OfferInteractions> written() {
        var batch = ArgumentCaptor.forClass(Collection.class);
        verify(metrics, times(1)).addInteractions(batch.capture());
        return new ArrayList<OfferInteractions>(batch.getValue());
    }

    private static final class MovableClock extends Clock {
        private Instant now;

        private MovableClock(Instant now) {
            this.now = now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
