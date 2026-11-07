package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.application.exceptions.MissingLocalCopyException;
import com.geopslabs.geops.catalog.application.usecases.ApplyComplianceEventUseCase.Outcome;
import com.geopslabs.geops.catalog.application.usecases.ComplianceEvent;
import com.geopslabs.geops.catalog.domain.models.ComplianceChange;
import com.geopslabs.geops.catalog.domain.models.MerchantStanding;
import com.geopslabs.geops.catalog.domain.ports.MerchantStandingRepositoryPort;
import com.geopslabs.geops.catalog.domain.ports.ReputationEventsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MerchantComplianceServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-30T19:11:00Z");
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-30T19:10:30Z");
    private static final Instant RUC_VERIFIED_AT = Instant.parse("2026-10-05T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Lima"));
    private static final long BUSINESS_ID = 84L;
    private static final String NAME = "Restaurante Don Pepe";
    private static final UUID EVENT_ID = UUID.fromString("7a6b5c4d-3e2f-4a1b-8c0d-9e8f7a6b5c4d");

    private MerchantStandingRepositoryPort standings;
    private ReputationEventsPort reputationEvents;
    private CellInvalidator cellInvalidator;
    private MerchantComplianceService service;

    @BeforeEach
    void setUp() {
        standings = mock(MerchantStandingRepositoryPort.class);
        reputationEvents = mock(ReputationEventsPort.class);
        cellInvalidator = mock(CellInvalidator.class);
        service = new MerchantComplianceService(standings, reputationEvents, cellInvalidator,
                TransactionOperations.withoutTransaction(), CLOCK);
        when(reputationEvents.markProcessed(any(), anyString(), any())).thenReturn(true);
        when(standings.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void anOpenedReportAddsAnOpenReportAndThenDeletesTheCellsOfTheBusiness() {
        stubStanding(verified(0, 0, 0));

        assertThat(service.apply(event("ReportOpened", ComplianceChange.forReportOpened()))).isEqualTo(Outcome.APPLIED);

        InOrder order = inOrder(reputationEvents, standings, cellInvalidator);
        order.verify(reputationEvents).markProcessed(EVENT_ID, "ReportOpened", NOW);
        var saved = ArgumentCaptor.forClass(MerchantStanding.class);
        order.verify(standings).save(saved.capture());
        order.verify(cellInvalidator).evictCellsOfBusiness(BUSINESS_ID);
        assertThat(saved.getValue().getOpenReports()).isEqualTo(1);
        assertThat(saved.getValue().hasVerifiedSeal()).isTrue();
        verify(reputationEvents, never()).recordSealChange(anyLong(), anyBoolean(), anyString(), any());
    }

    @Test
    void fallingBelowNinetyPercentRemovesTheSealAndRecordsTheReason() {
        stubStanding(verified(1, 8, 0));

        service.apply(event("ReportResolved", ComplianceChange.forReportResolved("OPEN", "CONSUMER_FAVOR")));

        var saved = ArgumentCaptor.forClass(MerchantStanding.class);
        verify(standings).save(saved.capture());
        assertThat(saved.getValue().getComplianceIndex()).isEqualByComparingTo("88.89");
        assertThat(saved.getValue().getOpenReports()).isZero();
        assertThat(saved.getValue().hasVerifiedSeal()).isFalse();
        verify(reputationEvents).recordSealChange(BUSINESS_ID, false, "COMPLIANCE_BELOW_THRESHOLD", OCCURRED_AT);
        verify(cellInvalidator).evictCellsOfBusiness(BUSINESS_ID);
    }

    @Test
    void recoveringTheThresholdRestoresTheSealAndRecordsTheReason() {
        stubStanding(verified(0, 8, 1));

        service.apply(event("ReservationRedeemed", ComplianceChange.forRedemption()));

        verify(reputationEvents).recordSealChange(BUSINESS_ID, true, "COMPLIANCE_RESTORED", OCCURRED_AT);
        verify(cellInvalidator).evictCellsOfBusiness(BUSINESS_ID);
    }

    @Test
    void aRedemptionThatChangesNeitherTheReportsNorTheSealKeepsTheCells() {
        stubStanding(verified(0, 10, 0));

        assertThat(service.apply(event("ReservationRedeemed", ComplianceChange.forRedemption())))
                .isEqualTo(Outcome.APPLIED);

        var saved = ArgumentCaptor.forClass(MerchantStanding.class);
        verify(standings).save(saved.capture());
        assertThat(saved.getValue().getRedeemedReservations()).isEqualTo(11);
        verifyNoInteractions(cellInvalidator);
    }

    @Test
    void aRepeatedEventChangesNothing() {
        when(reputationEvents.markProcessed(any(), anyString(), any())).thenReturn(false);

        assertThat(service.apply(event("ReportOpened", ComplianceChange.forReportOpened())))
                .isEqualTo(Outcome.DUPLICATE);

        verify(standings, never()).save(any());
        verifyNoInteractions(cellInvalidator);
    }

    @Test
    void aBusinessCatalogDoesNotKnowYetIsAMissingLocalCopy() {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(event("ReportOpened", ComplianceChange.forReportOpened())))
                .isInstanceOf(MissingLocalCopyException.class);
        verifyNoInteractions(cellInvalidator);
    }

    private void stubStanding(MerchantStanding standing) {
        when(standings.findByBusinessId(BUSINESS_ID)).thenReturn(Optional.of(standing));
    }

    private static MerchantStanding verified(int openReports, int redeemed, int consumerFavor) {
        var index = MerchantStanding.complianceIndexOf(redeemed, consumerFavor);
        return new MerchantStanding(BUSINESS_ID, NAME, true, openReports, index,
                MerchantStanding.sealFor(true, index), RUC_VERIFIED_AT, redeemed, consumerFavor);
    }

    private static ComplianceEvent event(String eventType, ComplianceChange change) {
        return new ComplianceEvent(EVENT_ID, eventType, OCCURRED_AT, BUSINESS_ID, change);
    }
}
