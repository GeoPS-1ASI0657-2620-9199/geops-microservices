package com.geopslabs.geops.reservation.domain.models;

import com.geopslabs.geops.reservation.domain.models.exceptions.ReportAlreadyClosedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportNotOpenException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportResponseDeadlinePassedException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * A consumer's report that the business did not honor an offer. It opens with a response deadline, the business may
 * answer once before it, and it closes either because the business resolved it in favor of the consumer or because
 * the deadline passed without an answer, which is also in favor of the consumer. The aggregate is immutable: each
 * transition returns the next state and rejects what the report does not allow.
 */
public class NonComplianceReport {
    private final Long id;
    private final Long reservationId;
    private final Long consumerId;
    private final Long businessId;
    private final ReportReason reason;
    private final LocalDateTime openedAt;
    private final LocalDateTime responseDeadline;
    private final ReportStatus status;
    private final ReportResolution resolution;
    private final LocalDateTime resolvedAt;
    private final ReportAnswer answer;

    @SuppressWarnings("java:S107")
    public NonComplianceReport(Long id, Long reservationId, Long consumerId, Long businessId, ReportReason reason,
                               LocalDateTime openedAt, LocalDateTime responseDeadline, ReportStatus status,
                               ReportResolution resolution, LocalDateTime resolvedAt, ReportAnswer answer) {
        this.id = id;
        this.reservationId = reservationId;
        this.consumerId = consumerId;
        this.businessId = businessId;
        this.reason = reason;
        this.openedAt = openedAt;
        this.responseDeadline = responseDeadline;
        this.status = status;
        this.resolution = resolution;
        this.resolvedAt = resolvedAt;
        this.answer = answer;
    }

    /** The report of a reservation that {@link Reservation#report} just moved to REPORTED. */
    public static NonComplianceReport open(Reservation reported, ReportReason reason, LocalDateTime now,
                                           Duration responseWindow) {
        return new NonComplianceReport(null, reported.getId(), reported.getConsumerId(), reported.getBusinessId(),
                reason, now, now.plus(responseWindow), ReportStatus.OPEN, null, null, null);
    }

    public boolean isOfBusiness(Long candidateBusinessId) {
        return businessId.equals(candidateBusinessId);
    }

    public boolean isOfConsumer(Long candidateConsumerId) {
        return consumerId.equals(candidateConsumerId);
    }

    /** The deadline is exclusive for the business: at that very instant it can no longer answer. */
    public boolean isPastDeadline(LocalDateTime now) {
        return !now.isBefore(responseDeadline);
    }

    /** Only an OPEN report before its deadline takes an answer; it moves to IN_REVIEW. */
    public NonComplianceReport answer(String text, LocalDateTime now) {
        if (status != ReportStatus.OPEN) {
            throw new ReportNotOpenException(id, status);
        }
        if (isPastDeadline(now)) {
            throw new ReportResponseDeadlinePassedException(id);
        }
        return new NonComplianceReport(id, reservationId, consumerId, businessId, reason, openedAt, responseDeadline,
                ReportStatus.IN_REVIEW, null, null, new ReportAnswer(null, text, now));
    }

    /** From OPEN or IN_REVIEW to CLOSED, with the resolution the caller is allowed to give. */
    public NonComplianceReport resolve(ReportResolution newResolution, LocalDateTime now) {
        if (status == ReportStatus.CLOSED) {
            throw new ReportAlreadyClosedException(id);
        }
        return new NonComplianceReport(id, reservationId, consumerId, businessId, reason, openedAt, responseDeadline,
                ReportStatus.CLOSED, newResolution, now, answer);
    }

    /** The business stayed silent: the report is resolved in favor of the consumer. */
    public NonComplianceReport closeByDeadline(LocalDateTime now) {
        if (status != ReportStatus.OPEN) {
            throw new ReportNotOpenException(id, status);
        }
        if (!isPastDeadline(now)) {
            throw new IllegalStateException("Report %d still has time to be answered".formatted(id));
        }
        return resolve(ReportResolution.CONSUMER_FAVOR, now);
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public LocalDateTime getResponseDeadline() {
        return responseDeadline;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public ReportResolution getResolution() {
        return resolution;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public ReportAnswer getAnswer() {
        return answer;
    }
}
