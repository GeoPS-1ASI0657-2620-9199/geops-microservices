package com.geopslabs.geops.reservation.domain.models.queries;

/** Who asks: the consumer or the business of the token, never both. */
public record GetReportQuery(Long reportId, Long consumerId, Long businessId) {
}
