package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.events.DomainEvent;
import com.geopslabs.geops.reservation.domain.events.ReportAnswered;
import com.geopslabs.geops.reservation.domain.events.ReportOpened;
import com.geopslabs.geops.reservation.domain.events.ReportResolved;
import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportReason;
import com.geopslabs.geops.reservation.domain.models.ReportResolution;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.ReservationStatus;
import com.geopslabs.geops.reservation.domain.models.commands.AnswerReportCommand;
import com.geopslabs.geops.reservation.domain.models.commands.OpenReportCommand;
import com.geopslabs.geops.reservation.domain.models.commands.ResolveReportCommand;
import com.geopslabs.geops.reservation.domain.models.exceptions.InvalidReportReasonException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportAccessDeniedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportAlreadyExistsException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportNotFoundException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportResponseDeadlinePassedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAccessDeniedException;
import com.geopslabs.geops.reservation.domain.ports.NonComplianceReportRepositoryPort;
import com.geopslabs.geops.reservation.domain.ports.OutboxPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCommandServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-28T19:10:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.parse("2026-10-28T19:10:00");
    private static final UUID EVENT_ID = UUID.fromString("9d8c7b6a-5f4e-4d3c-8b2a-1f0e9d8c7b6a");
    private static final String REASON = "No me aplicaron el 2x1 y me cobraron los dos platos.";
    private static final String ANSWER = "El 2x1 aplicaba solo de lunes a jueves.";

    @Mock
    private ReservationRepositoryPort reservations;
    @Mock
    private NonComplianceReportRepositoryPort reports;
    @Mock
    private OutboxPort outbox;

    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private ReportCommandService service;

    @BeforeEach
    void setUp() {
        service = new ReportCommandService(reservations, reports, outbox, clock, () -> EVENT_ID, Duration.ofHours(48));
    }

    @Test
    void opensTheReportWithItsDeadlineMarksTheReservationAndWritesTheEvent() {
        when(reservations.findByIdForUpdate(5013L)).thenReturn(Optional.of(reservation(ReservationStatus.ACTIVE)));
        when(reports.save(any(NonComplianceReport.class))).thenAnswer(call -> withId(call.getArgument(0)));

        var view = service.open(new OpenReportCommand(5013L, 2001L, REASON));

        var savedReservation = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).save(savedReservation.capture());
        assertThat(savedReservation.getValue().getStatus()).isEqualTo(ReservationStatus.REPORTED);
        assertThat(view.report().getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(view.report().getResponseDeadline()).isEqualTo(NOW_LOCAL.plusHours(48));
        assertThat(view.reservationCode()).isEqualTo("Q4ZT8K2M");
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        assertThat(event.getValue()).isInstanceOf(ReportOpened.class);
        assertThat(event.getValue().aggregateId()).isEqualTo(5013L);
        assertThat(event.getValue().eventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void anotherConsumerCannotReportAndNothingIsWritten() {
        when(reservations.findByIdForUpdate(5013L)).thenReturn(Optional.of(reservation(ReservationStatus.ACTIVE)));

        assertThatThrownBy(() -> service.open(new OpenReportCommand(5013L, 2002L, REASON)))
                .isInstanceOf(ReservationAccessDeniedException.class);

        verify(reports, never()).save(any());
        verify(outbox, never()).append(any());
    }

    @Test
    void aReasonThatIsTooShortIsRejectedBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> service.open(new OpenReportCommand(5013L, 2001L, "corto")))
                .isInstanceOf(InvalidReportReasonException.class);

        verify(reservations, never()).findByIdForUpdate(any());
    }

    @Test
    void aReservationThatWasAlreadyReportedWritesNoSecondReportOrEvent() {
        when(reservations.findByIdForUpdate(5013L)).thenReturn(Optional.of(reservation(ReservationStatus.REPORTED)));

        assertThatThrownBy(() -> service.open(new OpenReportCommand(5013L, 2001L, REASON)))
                .isInstanceOf(ReportAlreadyExistsException.class);

        verify(reports, never()).save(any());
        verify(outbox, never()).append(any());
    }

    @Test
    void answersWithinTheDeadlineAndWritesReportAnswered() {
        when(reports.findByIdForUpdate(31L)).thenReturn(Optional.of(openReport()));
        when(reports.save(any(NonComplianceReport.class))).thenAnswer(call -> call.getArgument(0));
        when(reservations.findById(5013L)).thenReturn(Optional.of(reservation(ReservationStatus.REPORTED)));

        var view = service.answer(new AnswerReportCommand(31L, 84L, ANSWER));

        assertThat(view.report().getStatus()).isEqualTo(ReportStatus.IN_REVIEW);
        assertThat(view.report().getAnswer().text()).isEqualTo(ANSWER);
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        assertThat(event.getValue()).isInstanceOf(ReportAnswered.class);
    }

    @Test
    void anAnswerAfterTheDeadlineIsRejectedAndNothingIsWritten() {
        var late = Clock.fixed(Instant.parse("2026-10-30T19:11:00Z"), ZoneOffset.UTC);
        service = new ReportCommandService(reservations, reports, outbox, late, () -> EVENT_ID, Duration.ofHours(48));
        when(reports.findByIdForUpdate(31L)).thenReturn(Optional.of(openReport()));

        assertThatThrownBy(() -> service.answer(new AnswerReportCommand(31L, 84L, ANSWER)))
                .isInstanceOf(ReportResponseDeadlinePassedException.class);

        verify(reports, never()).save(any());
        verify(outbox, never()).append(any());
    }

    @Test
    void anotherBusinessCannotAnswerOrResolveAndAnUnknownReportIsNotFound() {
        when(reports.findByIdForUpdate(31L)).thenReturn(Optional.of(openReport()));
        when(reports.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.answer(new AnswerReportCommand(31L, 99L, ANSWER)))
                .isInstanceOf(ReportAccessDeniedException.class);
        assertThatThrownBy(() -> service.resolve(new ResolveReportCommand(31L, 99L, ReportResolution.CONSUMER_FAVOR)))
                .isInstanceOf(ReportAccessDeniedException.class);
        assertThatThrownBy(() -> service.answer(new AnswerReportCommand(99L, 84L, ANSWER)))
                .isInstanceOf(ReportNotFoundException.class);
        verify(outbox, never()).append(any());
    }

    @Test
    void resolvingInFavorOfTheConsumerWritesReportResolvedWithThePreviousStatus() {
        var inReview = openReport().answer(ANSWER, NOW_LOCAL.minusHours(1));
        when(reports.findByIdForUpdate(31L)).thenReturn(Optional.of(inReview));
        when(reports.save(any(NonComplianceReport.class))).thenAnswer(call -> call.getArgument(0));
        when(reservations.findById(5013L)).thenReturn(Optional.of(reservation(ReservationStatus.REPORTED)));

        var view = service.resolve(new ResolveReportCommand(31L, 84L, ReportResolution.CONSUMER_FAVOR));

        assertThat(view.report().getStatus()).isEqualTo(ReportStatus.CLOSED);
        var event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outbox).append(event.capture());
        var resolved = (ReportResolved) event.getValue();
        assertThat(resolved.payload().previousStatus()).isEqualTo("IN_REVIEW");
        assertThat(resolved.payload().closedBy()).isEqualTo("BUSINESS");
    }

    @Test
    void theBusinessCannotResolveInItsOwnFavor() {
        assertThatThrownBy(() -> service.resolve(new ResolveReportCommand(31L, 84L, ReportResolution.BUSINESS_FAVOR)))
                .isInstanceOf(ReportAccessDeniedException.class)
                .hasMessageContaining("administrator");

        verify(reports, never()).save(any());
    }

    private static Reservation reservation(ReservationStatus status) {
        return new Reservation(5013L, new ReservationCode("Q4ZT8K2M"), 2001L, 1052L, 84L,
                "2x1 en almuerzos ejecutivos", NOW_LOCAL.minusDays(1), NOW_LOCAL.plusDays(1), null, status);
    }

    private static NonComplianceReport openReport() {
        return new NonComplianceReport(31L, 5013L, 2001L, 84L, new ReportReason(REASON), NOW_LOCAL.minusHours(2),
                NOW_LOCAL.plusHours(46), ReportStatus.OPEN, null, null, null);
    }

    private static NonComplianceReport withId(NonComplianceReport report) {
        return new NonComplianceReport(31L, report.getReservationId(), report.getConsumerId(), report.getBusinessId(),
                report.getReason(), report.getOpenedAt(), report.getResponseDeadline(), report.getStatus(), null, null,
                null);
    }
}
