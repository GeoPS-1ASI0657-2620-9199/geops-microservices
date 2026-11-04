package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.commands.OpenReportCommand;

public interface OpenReportUseCase {
    ReportView open(OpenReportCommand command);
}
