package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportAlreadyExistsException;
import com.geopslabs.geops.reservation.domain.ports.NonComplianceReportRepositoryPort;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NonComplianceReportJpaAdapter implements NonComplianceReportRepositoryPort {
    static final String ONE_REPORT_PER_RESERVATION = "uk_noncompliance_reports_reservation_id";

    private final NonComplianceReportJpaRepository reports;
    private final ReportAnswerJpaRepository answers;

    public NonComplianceReportJpaAdapter(NonComplianceReportJpaRepository reports, ReportAnswerJpaRepository answers) {
        this.reports = reports;
        this.answers = answers;
    }

    @Override
    public NonComplianceReport save(NonComplianceReport report) {
        try {
            var saved = reports.saveAndFlush(ReportPersistenceMapper.toEntity(report));
            var answer = report.getAnswer() == null ? null
                    : answers.saveAndFlush(ReportPersistenceMapper.toEntity(saved.getId(), report.getAnswer()));
            return ReportPersistenceMapper.toDomain(saved, answer);
        } catch (DataIntegrityViolationException exception) {
            var cause = NestedExceptionUtils.getMostSpecificCause(exception).getMessage();
            if (cause != null && cause.contains(ONE_REPORT_PER_RESERVATION)) {
                throw new ReportAlreadyExistsException(report.getReservationId());
            }
            throw exception;
        }
    }

    @Override
    public Optional<NonComplianceReport> findById(Long id) {
        return reports.findById(id).map(this::withAnswer);
    }

    @Override
    public Optional<NonComplianceReport> findByIdForUpdate(Long id) {
        return reports.findByIdForUpdate(id).map(this::withAnswer);
    }

    @Override
    public List<NonComplianceReport> findByBusiness(Long businessId, ReportStatus status) {
        return withAnswers(status == null ? reports.findByBusinessIdOrderByOpenedAtDesc(businessId)
                : reports.findByBusinessIdAndStatusOrderByOpenedAtDesc(businessId, status));
    }

    @Override
    public List<NonComplianceReport> findByConsumer(Long consumerId, ReportStatus status) {
        return withAnswers(status == null ? reports.findByConsumerIdOrderByOpenedAtDesc(consumerId)
                : reports.findByConsumerIdAndStatusOrderByOpenedAtDesc(consumerId, status));
    }

    @Override
    public List<NonComplianceReport> findByReservationIds(Collection<Long> reservationIds) {
        return reservationIds.isEmpty() ? List.of() : withAnswers(reports.findByReservationIdIn(reservationIds));
    }

    @Override
    public List<Long> findOpenIdsPastDeadline(LocalDateTime now, int limit) {
        return reports.findOpenIdsPastDeadline(now.toInstant(ZoneOffset.UTC), ReportStatus.OPEN,
                PageRequest.of(0, limit));
    }

    private NonComplianceReport withAnswer(NonComplianceReportJpaEntity entity) {
        return ReportPersistenceMapper.toDomain(entity, answers.findByReportId(entity.getId()).orElse(null));
    }

    /** One query for the answers of the whole page, not one per report. */
    private List<NonComplianceReport> withAnswers(List<NonComplianceReportJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<Long, ReportAnswerJpaEntity> byReport = answers
                .findByReportIdIn(entities.stream().map(NonComplianceReportJpaEntity::getId).toList()).stream()
                .collect(Collectors.toMap(ReportAnswerJpaEntity::getReportId, Function.identity()));
        return entities.stream().map(entity -> ReportPersistenceMapper.toDomain(entity, byReport.get(entity.getId())))
                .toList();
    }
}
