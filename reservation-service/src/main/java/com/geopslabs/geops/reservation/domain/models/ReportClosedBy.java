package com.geopslabs.geops.reservation.domain.models;

/** Who closed the report: the business owner, or the deadline scheduler when nobody answered. */
public enum ReportClosedBy {
    BUSINESS,
    DEADLINE
}
