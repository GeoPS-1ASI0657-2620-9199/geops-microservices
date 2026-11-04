package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;

import java.util.Collection;
import java.util.Map;

/** The report of each reservation of a page, by reservation id, loaded in one query. */
public interface FindReportsOfReservationsUseCase {
    Map<Long, NonComplianceReport> findByReservationIds(Collection<Long> reservationIds);
}
