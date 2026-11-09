package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.events.DomainEvent;
import com.geopslabs.geops.reservation.domain.events.ReportResolved;
import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportReason;
import com.geopslabs.geops.reservation.domain.models.ReportResolution;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.domain.ports.JobLockPort;
import com.geopslabs.geops.reservation.domain.ports.NonComplianceReportRepositoryPort;
import com.geopslabs.geops.reservation.domain.ports.OutboxPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadlineSchedulerTest {
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-10-30T19:15:00");
    private static final LocalDateTime DEADLINE = LocalDateTime.parse("2026-10-30T19:10:00");

    @Mock
    private ReservationRepositoryPort reservations;
    @Mock
    private NonComplianceReportRepositoryPort reports;
    @Mock
    private OutboxPort outbox;
    @Mock
    private JobLockPort lock;
    @Mock
    private PlatformTransactionManager transactionManager;

    private DeadlineScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        scheduler = new DeadlineScheduler(reservations, reports, outbox, lock,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC), () -> UUID.randomUUID(), transactionManager);
    }

    @Test
    void anotherReplicaHoldingTheLockMeansNothingIsDone() {
        when(lock.tryLock("reservation.deadline-scheduler")).thenReturn(false);

        scheduler.runOnce();

        verify(reservations, never()).expireActiveUntil(any());
        verify(reports, never()).findOpenIdsPastDeadline(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void expiresTheActiveReservationsAndClosesTheOpenReportInFavorOfTheConsumerWithItsEvent() {
        when(lock.tryLock(any())).thenReturn(true);
        when(reports.findOpenIdsPastDeadline(NOW, 100)).thenReturn(List.of(31L));
        when(reports.findByIdForUpdate(31L)).thenReturn(Optional.of(report(ReportStatus.OPEN)));
        when(reports.save(any(NonComplianceReport.class))).thenAnswer(call -> call.getArgument(0));
        when(reservations.findById(5013L)).thenReturn(Optional.of(reservation()));

        scheduler.runOnce();

        verify(reservations).expireActiveUntil(NOW);
        var saved = ArgumentCaptor.forClass(NonComplianceReport.class);
        verify(reports).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ReportStatus.CLOSED);
        assertThat(saved.getValue().getResolution()).isEqualTo(ReportResolution.CONSUMER_FAVOR);
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        var resolved = (ReportResolved) event.getValue();
        assertThat(resolved.payload().closedBy()).isEqualTo("DEADLINE");
        assertThat(resolved.payload().previousStatus()).isEqualTo("OPEN");
    }

    @Test
    void aReportAnsweredAMomentBeforeIsLeftAloneBecauseTheRowIsReadAgainUnderLock() {
        when(lock.tryLock(any())).thenReturn(true);
        when(reports.findOpenIdsPastDeadline(NOW, 100)).thenReturn(List.of(31L));
        when(reports.findByIdForUpdate(31L)).thenReturn(Optional.of(report(ReportStatus.IN_REVIEW)));

        scheduler.runOnce();

        verify(reports, never()).save(any());
        verify(outbox, never()).append(any());
    }

    @Test
    void aFailureClosingOneReportDoesNotStopTheNextOne() {
        when(lock.tryLock(any())).thenReturn(true);
        when(reports.findOpenIdsPastDeadline(NOW, 100)).thenReturn(List.of(30L, 31L));
        when(reports.findByIdForUpdate(30L)).thenThrow(new IllegalStateException("database hiccup"));
        when(reports.findByIdForUpdate(31L)).thenReturn(Optional.of(report(ReportStatus.OPEN)));
        when(reports.save(any(NonComplianceReport.class))).thenAnswer(call -> call.getArgument(0));
        when(reservations.findById(5013L)).thenReturn(Optional.of(reservation()));

        scheduler.runOnce();

        verify(outbox).append(any(ReportResolved.class));
    }

    private static NonComplianceReport report(ReportStatus status) {
        return new NonComplianceReport(31L, 5013L, 2001L, 84L, new ReportReason("No me aplicaron el 2x1."),
                DEADLINE.minusHours(48), DEADLINE, status, null, null, null);
    }

    private static Reservation reservation() {
        return new Reservation(5013L, new ReservationCode("Q4ZT8K2M"), 2001L, 1052L, 84L,
                "2x1 en almuerzos ejecutivos", DEADLINE.minusDays(3), DEADLINE.plusDays(1), null,
                ReservationStatus.REPORTED);
    }
}
