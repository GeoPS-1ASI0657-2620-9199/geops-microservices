package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.application.usecases.AnswerReportUseCase;
import com.geopslabs.geops.reservation.application.usecases.OpenReportUseCase;
import com.geopslabs.geops.reservation.application.usecases.ReportView;
import com.geopslabs.geops.reservation.application.usecases.ResolveReportUseCase;
import com.geopslabs.geops.reservation.domain.events.ReportAnswered;
import com.geopslabs.geops.reservation.domain.events.ReportOpened;
import com.geopslabs.geops.reservation.domain.events.ReportResolved;
import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportClosedBy;
import com.geopslabs.geops.reservation.domain.models.ReportReason;
import com.geopslabs.geops.reservation.domain.models.ReportResolution;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.AnswerReportCommand;
import com.geopslabs.geops.reservation.domain.models.commands.OpenReportCommand;
import com.geopslabs.geops.reservation.domain.models.commands.ResolveReportCommand;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportAccessDeniedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportNotFoundException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationAccessDeniedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationNotFoundException;
import com.geopslabs.geops.reservation.domain.ports.NonComplianceReportRepositoryPort;
import com.geopslabs.geops.reservation.domain.ports.OutboxPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Opens, answers and resolves noncompliance reports. Every transition and its event share one transaction, so a
 * report never changes without its event reaching the outbox, and the row locks make a redemption, an answer and
 * the deadline scheduler of the same reservation take turns.
 */
public class ReportCommandService implements OpenReportUseCase, AnswerReportUseCase, ResolveReportUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportCommandService.class);

    private final ReservationRepositoryPort reservations;
    private final NonComplianceReportRepositoryPort reports;
    private final OutboxPort outbox;
    private final Clock clock;
    private final Supplier<UUID> eventIds;
    private final Duration responseWindow;

    public ReportCommandService(ReservationRepositoryPort reservations, NonComplianceReportRepositoryPort reports,
                                OutboxPort outbox, Clock clock, Supplier<UUID> eventIds, Duration responseWindow) {
        this.reservations = reservations;
        this.reports = reports;
        this.outbox = outbox;
        this.clock = clock;
        this.eventIds = eventIds;
        this.responseWindow = responseWindow;
    }

    @Override
    @Transactional
    public ReportView open(OpenReportCommand command) {
        var reason = new ReportReason(command.reason());
        var reservation = reservations.findByIdForUpdate(command.reservationId())
                .orElseThrow(() -> ReservationNotFoundException.withId(command.reservationId()));
        if (!reservation.isOwnedByConsumer(command.consumerId())) {
            LOGGER.info("report.open.denied reservationId={} reason=other-consumer", reservation.getId());
            throw ReservationAccessDeniedException.otherConsumer(reservation.getId());
        }
        var reported = reservation.report(now());
        reservations.save(reported);
        var report = reports.save(NonComplianceReport.open(reported, reason, now(), responseWindow));
        outbox.append(ReportOpened.of(report, reported, eventIds.get()));
        LOGGER.info("report.opened reportId={} reservationId={} businessId={}", report.getId(), reported.getId(),
                report.getBusinessId());
        return view(report, reported);
    }

    @Override
    @Transactional
    public ReportView answer(AnswerReportCommand command) {
        var report = ownedByBusiness(command.reportId(), command.businessId());
        var answered = reports.save(report.answer(command.text(), now()));
        var reservation = reservationOf(answered);
        outbox.append(ReportAnswered.of(answered, reservation, eventIds.get()));
        LOGGER.info("report.answered reportId={} businessId={}", answered.getId(), answered.getBusinessId());
        return view(answered, reservation);
    }

    @Override
    @Transactional
    public ReportView resolve(ResolveReportCommand command) {
        if (command.resolution() != ReportResolution.CONSUMER_FAVOR) {
            throw ReportAccessDeniedException.businessFavorIsForTheAdministrator();
        }
        var report = ownedByBusiness(command.reportId(), command.businessId());
        var closed = reports.save(report.resolve(command.resolution(), now()));
        var reservation = reservationOf(closed);
        outbox.append(ReportResolved.of(closed, report.getStatus(), ReportClosedBy.BUSINESS, reservation,
                eventIds.get()));
        LOGGER.info("report.resolved reportId={} resolution={} previousStatus={}", closed.getId(),
                closed.getResolution(), report.getStatus());
        return view(closed, reservation);
    }

    private NonComplianceReport ownedByBusiness(Long reportId, Long businessId) {
        var report = reports.findByIdForUpdate(reportId).orElseThrow(() -> new ReportNotFoundException(reportId));
        if (!report.isOfBusiness(businessId)) {
            LOGGER.info("report.access-denied reportId={} reason=other-business", reportId);
            throw ReportAccessDeniedException.otherParty(reportId);
        }
        return report;
    }

    private Reservation reservationOf(NonComplianceReport report) {
        return reservations.findById(report.getReservationId())
                .orElseThrow(() -> ReservationNotFoundException.withId(report.getReservationId()));
    }

    private static ReportView view(NonComplianceReport report, Reservation reservation) {
        return new ReportView(report, reservation.getCode().value(), reservation.getOfferTitle());
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
