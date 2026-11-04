package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.NonComplianceReport;

/** A report with the data of its reservation that the screens show next to it. */
public record ReportView(NonComplianceReport report, String reservationCode, String offerTitle) {
}
