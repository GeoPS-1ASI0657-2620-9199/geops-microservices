package com.geopslabs.geops.reservation.infrastructure.web;

import java.time.Instant;

/** What the reservation shows of its report: that it exists, how it stands and until when the business can answer. */
public record ReservationReportSummary(Long reportId, String status, Instant responseDeadline) {
}
