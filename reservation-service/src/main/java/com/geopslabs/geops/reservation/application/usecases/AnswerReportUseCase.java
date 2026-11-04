package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.commands.AnswerReportCommand;

public interface AnswerReportUseCase {
    ReportView answer(AnswerReportCommand command);
}
