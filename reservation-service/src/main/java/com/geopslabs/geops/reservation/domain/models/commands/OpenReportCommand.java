package com.geopslabs.geops.reservation.domain.models.commands;

public record OpenReportCommand(Long reservationId, Long consumerId, String reason) {
}
