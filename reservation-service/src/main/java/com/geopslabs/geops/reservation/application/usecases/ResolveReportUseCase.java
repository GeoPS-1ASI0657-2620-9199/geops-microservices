package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.commands.ResolveReportCommand;

public interface ResolveReportUseCase {
    ReportView resolve(ResolveReportCommand command);
}
