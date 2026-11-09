package com.geopslabs.geops.reservation.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

/** Only {@code status = CLOSED} is accepted, with the resolution the caller is allowed to give. */
public record ChangeReportStatusRequest(@NotBlank String status, String resolution) {
}
