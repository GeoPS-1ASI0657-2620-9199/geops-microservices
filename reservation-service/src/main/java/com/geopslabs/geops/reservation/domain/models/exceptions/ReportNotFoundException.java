package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.NotFoundException;

public class ReportNotFoundException extends NotFoundException {
    private static final String CODE = "REPORT_NOT_FOUND";
    private static final String MESSAGE = "Report %d was not found";

    public ReportNotFoundException(Long reportId) {
        super(CODE, MESSAGE.formatted(reportId));
    }
}
