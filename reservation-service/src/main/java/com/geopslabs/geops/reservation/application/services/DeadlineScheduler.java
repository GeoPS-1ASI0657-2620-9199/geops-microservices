package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.events.ReportResolved;
import com.geopslabs.geops.reservation.domain.models.ReportClosedBy;
import com.geopslabs.geops.reservation.domain.ports.JobLockPort;
import com.geopslabs.geops.reservation.domain.ports.NonComplianceReportRepositoryPort;
import com.geopslabs.geops.reservation.domain.ports.OutboxPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The two jobs of time (AC09): active reservations past their deadline become EXPIRED, and an open report whose
 * response deadline passed without an answer is closed in favor of the consumer. An advisory lock lets only one
 * replica run it, each report is closed in its own transaction, and running it twice changes nothing.
 */
public class DeadlineScheduler {
    static final String LOCK = "reservation.deadline-scheduler";
    static final int REPORTS_PER_ROUND = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeadlineScheduler.class);

    private final ReservationRepositoryPort reservations;
    private final NonComplianceReportRepositoryPort reports;
    private final OutboxPort outbox;
    private final JobLockPort lock;
    private final Clock clock;
    private final Supplier<UUID> eventIds;
    private final TransactionTemplate round;
    private final TransactionTemplate perReport;

    public DeadlineScheduler(ReservationRepositoryPort reservations, NonComplianceReportRepositoryPort reports,
                             OutboxPort outbox, JobLockPort lock, Clock clock, Supplier<UUID> eventIds,
                             PlatformTransactionManager transactionManager) {
        this.reservations = reservations;
        this.reports = reports;
        this.outbox = outbox;
        this.lock = lock;
        this.clock = clock;
        this.eventIds = eventIds;
        this.round = new TransactionTemplate(transactionManager);
        this.perReport = new TransactionTemplate(transactionManager);
        this.perReport.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void runOnce() {
        var now = now();
        var closed = round.execute(status -> {
            if (!lock.tryLock(LOCK)) {
                LOGGER.debug("reservation.deadline-scheduler.skipped reason=another-replica-holds-the-lock");
                return 0;
            }
            var expired = reservations.expireActiveUntil(now);
            LOGGER.info("reservation.reservations.expired count={}", expired);
            return closeReportsPastDeadline(now);
        });
        LOGGER.info("reservation.reports.closed-by-deadline count={}", closed);
    }

    private int closeReportsPastDeadline(LocalDateTime now) {
        var closed = 0;
        for (var reportId : reports.findOpenIdsPastDeadline(now, REPORTS_PER_ROUND)) {
            try {
                closed += perReport.execute(status -> closeIfStillOpen(reportId, now));
            } catch (RuntimeException failure) {
                LOGGER.error("reservation.report.close-by-deadline.failed reportId={}", reportId, failure);
            }
        }
        return closed;
    }

    /** The row is locked and read again: an answer that committed a moment ago keeps the report out of the closing. */
    private int closeIfStillOpen(Long reportId, LocalDateTime now) {
        var report = reports.findByIdForUpdate(reportId).orElse(null);
        if (report == null || report.getStatus() != com.geopslabs.geops.reservation.domain.models.ReportStatus.OPEN || !report.isPastDeadline(now)) {
            return 0;
        }
        var closed = reports.save(report.closeByDeadline(now));
        var reservation = reservations.findById(closed.getReservationId()).orElseThrow();
        outbox.append(ReportResolved.of(closed, report.getStatus(), ReportClosedBy.DEADLINE, reservation,
                eventIds.get()));
        return 1;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
