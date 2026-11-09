package com.geopslabs.geops.reservation.domain.models.commands;

import com.geopslabs.geops.reservation.domain.models.ReportResolution;

/** Who resolves, and in whose favor. The business owner can only resolve in favor of the consumer. */
public record ResolveReportCommand(Long reportId, Long businessId, ReportResolution resolution) {
}
