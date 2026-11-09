package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;

public class ReportAlreadyClosedException extends ConflictException {
    private static final String CODE = "REPORT_ALREADY_CLOSED";
    private static final String MESSAGE = "Report %d is already closed";

    public ReportAlreadyClosedException(Long reportId) {
        super(CODE, MESSAGE.formatted(reportId));
    }
}
