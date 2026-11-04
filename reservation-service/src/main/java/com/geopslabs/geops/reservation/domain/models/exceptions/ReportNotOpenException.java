package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.domain.models.ReportStatus;
import com.geopslabs.geops.reservation.shared.domain.ConflictException;

public class ReportNotOpenException extends ConflictException {
    private static final String CODE = "REPORT_NOT_OPEN";
    private static final String MESSAGE = "Report %d is %s and cannot be answered";

    public ReportNotOpenException(Long reportId, ReportStatus status) {
        super(CODE, MESSAGE.formatted(reportId, status));
    }
}
