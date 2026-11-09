package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.GoneException;

public class ReportResponseDeadlinePassedException extends GoneException {
    private static final String CODE = "REPORT_RESPONSE_DEADLINE_PASSED";
    private static final String MESSAGE = "The deadline to answer report %d has passed";

    public ReportResponseDeadlinePassedException(Long reportId) {
        super(CODE, MESSAGE.formatted(reportId));
    }
}
