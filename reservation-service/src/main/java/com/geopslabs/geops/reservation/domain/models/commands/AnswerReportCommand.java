package com.geopslabs.geops.reservation.domain.models.commands;

public record AnswerReportCommand(Long reportId, Long businessId, String text) {
}
