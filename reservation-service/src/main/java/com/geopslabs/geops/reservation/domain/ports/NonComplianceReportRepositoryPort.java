package com.geopslabs.geops.reservation.domain.ports;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.ReportStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NonComplianceReportRepositoryPort {
    /** Inserts or updates the report and its answer, and returns it as stored. */
    NonComplianceReport save(NonComplianceReport report);

    Optional<NonComplianceReport> findById(Long id);

    /** Locks the row until the end of the transaction, so an answer and the deadline scheduler take turns. */
    Optional<NonComplianceReport> findByIdForUpdate(Long id);

    List<NonComplianceReport> findByBusiness(Long businessId, ReportStatus status);

    List<NonComplianceReport> findByConsumer(Long consumerId, ReportStatus status);

    List<NonComplianceReport> findByReservationIds(Collection<Long> reservationIds);

    /** Ids of the OPEN reports whose response deadline is not after {@code now}, oldest deadline first. */
    List<Long> findOpenIdsPastDeadline(LocalDateTime now, int limit);
}
