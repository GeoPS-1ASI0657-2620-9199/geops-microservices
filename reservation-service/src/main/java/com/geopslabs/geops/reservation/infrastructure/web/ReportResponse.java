package com.geopslabs.geops.reservation.infrastructure.web;

import java.time.Instant;

public record ReportResponse(
        Long reportId,
        Long reservationId,
        String reservationCode,
        String offerTitle,
        String reason,
        String status,
        Instant openedAt,
        Instant responseDeadline,
        Answer response,
        String resolution,
        Instant resolvedAt) {

    public record Answer(String text, Instant respondedAt) {
    }
}
