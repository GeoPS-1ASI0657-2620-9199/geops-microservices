package com.geopslabs.geops.reservation.infrastructure.persistence;

import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NonComplianceReportJpaRepository extends JpaRepository<NonComplianceReportJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM NonComplianceReportJpaEntity r WHERE r.id = :id")
    Optional<NonComplianceReportJpaEntity> findByIdForUpdate(@Param("id") Long id);

    List<NonComplianceReportJpaEntity> findByBusinessIdOrderByOpenedAtDesc(Long businessId);

    List<NonComplianceReportJpaEntity> findByBusinessIdAndStatusOrderByOpenedAtDesc(Long businessId,
                                                                                    ReportStatus status);

    List<NonComplianceReportJpaEntity> findByConsumerIdOrderByOpenedAtDesc(Long consumerId);

    List<NonComplianceReportJpaEntity> findByConsumerIdAndStatusOrderByOpenedAtDesc(Long consumerId,
                                                                                    ReportStatus status);

    List<NonComplianceReportJpaEntity> findByReservationIdIn(Collection<Long> reservationIds);

    @Query("""
            SELECT r.id FROM NonComplianceReportJpaEntity r
             WHERE r.status = :open AND r.responseDeadline <= :now
             ORDER BY r.responseDeadline""")
    List<Long> findOpenIdsPastDeadline(@Param("now") Instant now, @Param("open") ReportStatus open, Pageable page);
}
