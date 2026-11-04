package com.geopslabs.geops.reservation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportAnswerJpaRepository extends JpaRepository<ReportAnswerJpaEntity, Long> {
    Optional<ReportAnswerJpaEntity> findByReportId(Long reportId);

    List<ReportAnswerJpaEntity> findByReportIdIn(Collection<Long> reportIds);
}
