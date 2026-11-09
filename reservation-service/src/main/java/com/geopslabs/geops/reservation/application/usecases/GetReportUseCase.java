package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.queries.GetReportQuery;

public interface GetReportUseCase {
    ReportView get(GetReportQuery query);
}
