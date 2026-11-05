package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.domain.events.DomainEvent;
import com.geopslabs.geops.catalog.domain.events.OfferExpired;
import com.geopslabs.geops.catalog.domain.models.ExpiredOffer;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import com.geopslabs.geops.catalog.domain.ports.OfferRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.OutboxPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferExpirationSchedulerTest {
    private static final Instant NOW = Instant.parse("2026-11-16T05:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Lima"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 11, 15);
    private static final GeoPoint MIRAFLORES = new GeoPoint(-12.1211, -77.0297);
    private static final Long CAMPAIGN_ID = 77L;
    private static final Long BUSINESS_ID = 84L;
    private static final Long CAMPAIGN_OFFER_ID = 1052L;
    private static final Long PUBLIC_OFFER_ID = 2001L;

    @Mock
    private OfferRepositoryPort offers;
    @Mock
    private JobLockPort jobLock;
    @Mock
    private OutboxPort outbox;
    @Mock
    private CellInvalidator cellInvalidator;

    private OfferExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OfferExpirationScheduler(offers, jobLock, outbox, cellInvalidator,
                TransactionOperations.withoutTransaction(), CLOCK);
    }

    @Test
    void eachExpiredCampaignOfferIsAnnouncedWithOfferExpired() {
        when(jobLock.tryLock(OfferExpirationScheduler.LOCK)).thenReturn(true);
        when(offers.expirePublishedBefore(TODAY)).thenReturn(List.of(
                new ExpiredOffer(CAMPAIGN_OFFER_ID, CAMPAIGN_ID, BUSINESS_ID, VALID_TO, MIRAFLORES),
                new ExpiredOffer(PUBLIC_OFFER_ID, null, null, VALID_TO, null)));

        assertThat(scheduler.expireOffers()).isEqualTo(1);

        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        var expired = (OfferExpired) event.getValue();
        assertThat(expired.aggregateId()).isEqualTo(CAMPAIGN_ID);
        assertThat(expired.occurredAt()).isEqualTo(NOW);
        assertThat(expired.payload().offerId()).isEqualTo(CAMPAIGN_OFFER_ID);
        assertThat(expired.payload().businessId()).isEqualTo(BUSINESS_ID);
        assertThat(expired.payload().validTo()).isEqualTo(VALID_TO);
        assertThat(expired.payload().expiredAt()).isEqualTo(NOW);
        verify(cellInvalidator).evictCellsOf(List.of(MIRAFLORES));
    }

    @Test
    void aRoundWithoutExpiredOffersWritesNothing() {
        when(jobLock.tryLock(OfferExpirationScheduler.LOCK)).thenReturn(true);
        when(offers.expirePublishedBefore(TODAY)).thenReturn(List.of());

        assertThat(scheduler.expireOffers()).isZero();

        verifyNoInteractions(outbox);
    }

    @Test
    void anotherReplicaHoldingTheLockWritesNothing() {
        when(jobLock.tryLock(OfferExpirationScheduler.LOCK)).thenReturn(false);

        assertThat(scheduler.expireOffers()).isEqualTo(OfferExpirationScheduler.SKIPPED);

        verifyNoInteractions(offers, outbox, cellInvalidator);
    }
}
