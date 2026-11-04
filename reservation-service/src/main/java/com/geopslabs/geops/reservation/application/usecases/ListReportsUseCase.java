package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.queries.ListReportsQuery;

import java.util.List;

public interface ListReportsUseCase {
    List<ReportView> list(ListReportsQuery query);
}
