package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.application.usecases.ReportView;
import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class ReportResponseAssembler {

    private ReportResponseAssembler() {
    }

    public static ReportResponse toResponse(ReportView view) {
        var report = view.report();
        var answer = report.getAnswer() == null ? null
                : new ReportResponse.Answer(report.getAnswer().text(), toInstant(report.getAnswer().respondedAt()));
        return new ReportResponse(report.getId(), report.getReservationId(), view.reservationCode(),
                view.offerTitle(), report.getReason().value(), report.getStatus().name(),
                toInstant(report.getOpenedAt()), toInstant(report.getResponseDeadline()), answer,
                report.getResolution() == null ? null : report.getResolution().name(),
                toInstant(report.getResolvedAt()));
    }

    public static ReservationReportSummary toSummary(NonComplianceReport report) {
        return report == null ? null : new ReservationReportSummary(report.getId(), report.getStatus().name(),
                toInstant(report.getResponseDeadline()));
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
