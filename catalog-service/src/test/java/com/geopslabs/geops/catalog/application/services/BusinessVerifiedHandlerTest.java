package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.usecases.ApplyBusinessVerifiedUseCase.Outcome;
import com.geopslabs.geops.catalog.application.usecases.BusinessVerifiedEvent;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ReputationEventsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BusinessVerifiedHandlerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-08T15:00:00Z"), ZoneId.of("America/Lima"));
    private static final Instant MONDAY = Instant.parse("2026-10-05T15:00:00Z");
    private static final Instant TUESDAY = Instant.parse("2026-10-06T15:00:00Z");
    private static final long BUSINESS_ID = 84L;
    private static final String NAME = "Restaurante Don Pepe";

    private MerchantStandingRepositoryPort standings;
    private ReputationEventsPort reputationEvents;
    private CellInvalidator cellInvalidator;
    private MerchantReputationService service;

    @BeforeEach
    void setUp() {
        standings = mock(MerchantStandingRepositoryPort.class);
        reputationEvents = mock(ReputationEventsPort.class);
        cellInvalidator = mock(CellInvalidator.class);
        service = new MerchantReputationService(standings, reputationEvents, cellInvalidator,
                TransactionOperations.withoutTransaction(), CLOCK);
        when(reputationEvents.markProcessed(any(), anyString(), any())).thenReturn(true);
        when(standings.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aVerifiedRucGivesTheSealRecordsTheChangeAndThenDeletesTheCells() {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(standing(false, MONDAY)));

        assertThat(service.apply(event(true, TUESDAY))).isEqualTo(Outcome.APPLIED);

        InOrder order = inOrder(standings, reputationEvents, cellInvalidator);
        var saved = ArgumentCaptor.forClass(MerchantStanding.class);
        order.verify(standings).save(saved.capture());
        order.verify(reputationEvents).recordSealChange(BUSINESS_ID, true, "RUC_VERIFIED", TUESDAY);
        order.verify(cellInvalidator).evictCellsOfBusiness(BUSINESS_ID);
        assertThat(saved.getValue().hasVerifiedSeal()).isTrue();
    }

    @Test
    void losingTheVerificationRecordsTheLoss() {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(standing(true, MONDAY)));

        service.apply(event(false, TUESDAY));

        verify(reputationEvents).recordSealChange(BUSINESS_ID, false, "RUC_VERIFICATION_LOST", TUESDAY);
    }

    @Test
    void theSameEventTwiceChangesNothingTheSecondTime() {
        when(reputationEvents.markProcessed(any(), anyString(), any())).thenReturn(false);

        assertThat(service.apply(event(true, TUESDAY))).isEqualTo(Outcome.DUPLICATE);

        verify(standings, never()).save(any());
        verify(reputationEvents, never()).recordSealChange(anyLong(), anyBoolean(), anyString(), any());
        verifyNoInteractions(cellInvalidator);
    }

    @Test
    void anEventOlderThanTheCopyOnlyRecordsItsId() {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(standing(true, TUESDAY)));

        assertThat(service.apply(event(false, MONDAY))).isEqualTo(Outcome.STALE);

        verify(reputationEvents).markProcessed(any(), eq("BusinessVerified"), any());
        verify(standings, never()).save(any());
        verifyNoInteractions(cellInvalidator);
    }

    @Test
    void aBusinessWithoutStandingGetsOneFromTheEvent() {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        service.apply(event(true, TUESDAY));

        var saved = ArgumentCaptor.forClass(MerchantStanding.class);
        verify(standings).save(saved.capture());
        assertThat(saved.getValue().getBusinessName()).isEqualTo(NAME);
        assertThat(saved.getValue().getComplianceIndex()).isEqualByComparingTo("100.00");
        assertThat(saved.getValue().hasVerifiedSeal()).isTrue();
        verify(reputationEvents).recordSealChange(BUSINESS_ID, true, "RUC_VERIFIED", TUESDAY);
    }

    @Test
    void theSameAnswerAgainKeepsTheSealWithoutAChangeRow() {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(standing(true, MONDAY)));

        service.apply(event(true, TUESDAY));

        verify(reputationEvents, never()).recordSealChange(anyLong(), anyBoolean(), anyString(), any());
        verify(cellInvalidator).evictCellsOfBusiness(BUSINESS_ID);
    }

    private static MerchantStanding standing(boolean verified, Instant updatedAt) {
        return new MerchantStanding(BUSINESS_ID, NAME, verified, 0, new BigDecimal("100.00"), verified, updatedAt);
    }

    private static BusinessVerifiedEvent event(boolean verified, Instant occurredAt) {
        return new BusinessVerifiedEvent(UUID.randomUUID(), occurredAt, BUSINESS_ID, NAME, verified);
    }
}
