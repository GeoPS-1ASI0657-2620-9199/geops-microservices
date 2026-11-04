package com.geopslabs.geops.reservation.domain.models.queries;

import com.geopslabs.geops.reservation.domain.models.ReportStatus;

/** Exactly one of {@code consumerId} and {@code businessId} is set: the party of the token. */
public record ListReportsQuery(Long consumerId, Long businessId, ReportStatus status) {

    public static ListReportsQuery ofConsumer(Long consumerId, ReportStatus status) {
        return new ListReportsQuery(consumerId, null, status);
    }

    public static ListReportsQuery ofBusiness(Long businessId, ReportStatus status) {
        return new ListReportsQuery(null, businessId, status);
    }
}
