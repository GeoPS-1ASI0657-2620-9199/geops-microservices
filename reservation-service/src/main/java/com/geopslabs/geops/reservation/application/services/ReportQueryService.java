package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.application.usecases.FindReportsOfReservationsUseCase;
import com.geopslabs.geops.reservation.application.usecases.GetReportUseCase;
import com.geopslabs.geops.reservation.application.usecases.ListReportsUseCase;
import com.geopslabs.geops.reservation.application.usecases.ReportView;
import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportAccessDeniedException;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReportNotFoundException;
import com.geopslabs.geops.reservation.domain.models.queries.GetReportQuery;
import com.geopslabs.geops.reservation.domain.models.queries.ListReportsQuery;
import com.geopslabs.geops.reservation.domain.ports.NonComplianceReportRepositoryPort;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** What the consumer and the business owner see of the reports: only their own, newest first. */
public class ReportQueryService implements ListReportsUseCase, GetReportUseCase, FindReportsOfReservationsUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportQueryService.class);

    private final NonComplianceReportRepositoryPort reports;
    private final ReservationRepositoryPort reservations;

    public ReportQueryService(NonComplianceReportRepositoryPort reports, ReservationRepositoryPort reservations) {
        this.reports = reports;
        this.reservations = reservations;
    }

    @Override
    public List<ReportView> list(ListReportsQuery query) {
        var found = query.businessId() != null
                ? reports.findByBusiness(query.businessId(), query.status())
                : reports.findByConsumer(query.consumerId(), query.status());
        return views(found);
    }

    @Override
    public ReportView get(GetReportQuery query) {
        var report = reports.findById(query.reportId()).orElseThrow(() -> new ReportNotFoundException(query.reportId()));
        var allowed = query.businessId() != null ? report.isOfBusiness(query.businessId())
                : report.isOfConsumer(query.consumerId());
        if (!allowed) {
            LOGGER.info("report.access-denied reportId={} reason=other-party", report.getId());
            throw ReportAccessDeniedException.otherParty(report.getId());
        }
        return views(List.of(report)).get(0);
    }

    @Override
    public Map<Long, NonComplianceReport> findByReservationIds(Collection<Long> reservationIds) {
        return reports.findByReservationIds(reservationIds).stream()
                .collect(Collectors.toMap(NonComplianceReport::getReservationId, Function.identity()));
    }

    /** The reservations of the whole page come in one query, not one per report. */
    private List<ReportView> views(List<NonComplianceReport> found) {
        Map<Long, Reservation> byId = reservations
                .findAllByIds(found.stream().map(NonComplianceReport::getReservationId).toList()).stream()
                .collect(Collectors.toMap(Reservation::getId, Function.identity()));
        return found.stream().map(report -> {
            var reservation = byId.get(report.getReservationId());
            return new ReportView(report, reservation.getCode().value(), reservation.getOfferTitle());
        }).toList();
    }
}
