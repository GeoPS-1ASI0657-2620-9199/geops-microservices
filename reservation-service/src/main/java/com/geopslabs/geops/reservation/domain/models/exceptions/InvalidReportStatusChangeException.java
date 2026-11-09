package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.InvalidRequestException;

public class InvalidReportStatusChangeException extends InvalidRequestException {
    private static final String CODE = "INVALID_REQUEST";
    private static final String MESSAGE = "status must be CLOSED and resolution is required";

    public InvalidReportStatusChangeException() {
        super(CODE, MESSAGE);
    }
}
