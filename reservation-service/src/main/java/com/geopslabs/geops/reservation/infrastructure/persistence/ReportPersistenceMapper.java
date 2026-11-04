package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportAnswer;
import com.geopslabs.geops.reservation.domain.models.ReportReason;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

final class ReportPersistenceMapper {

    private ReportPersistenceMapper() {
    }

    static NonComplianceReport toDomain(NonComplianceReportJpaEntity entity, ReportAnswerJpaEntity answer) {
        return new NonComplianceReport(entity.getId(), entity.getReservationId(), entity.getConsumerId(),
                entity.getBusinessId(), new ReportReason(entity.getReason()), toUtc(entity.getOpenedAt()),
                toUtc(entity.getResponseDeadline()), entity.getStatus(), entity.getResolution(),
                toUtc(entity.getResolvedAt()), answer == null ? null
                        : new ReportAnswer(answer.getId(), answer.getText(), toUtc(answer.getRespondedAt())));
    }

    static NonComplianceReportJpaEntity toEntity(NonComplianceReport report) {
        var entity = new NonComplianceReportJpaEntity();
        entity.setId(report.getId());
        entity.setReservationId(report.getReservationId());
        entity.setConsumerId(report.getConsumerId());
        entity.setBusinessId(report.getBusinessId());
        entity.setReason(report.getReason().value());
        entity.setOpenedAt(toInstant(report.getOpenedAt()));
        entity.setResponseDeadline(toInstant(report.getResponseDeadline()));
        entity.setStatus(report.getStatus());
        entity.setResolution(report.getResolution());
        entity.setResolvedAt(toInstant(report.getResolvedAt()));
        return entity;
    }

    static ReportAnswerJpaEntity toEntity(Long reportId, ReportAnswer answer) {
        var entity = new ReportAnswerJpaEntity();
        entity.setId(answer.id());
        entity.setReportId(reportId);
        entity.setText(answer.text());
        entity.setRespondedAt(toInstant(answer.respondedAt()));
        return entity;
    }

    private static LocalDateTime toUtc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
