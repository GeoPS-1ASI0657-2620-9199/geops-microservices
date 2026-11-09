package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;

public class ReportAlreadyExistsException extends ConflictException {
    private static final String CODE = "REPORT_ALREADY_EXISTS";
    private static final String MESSAGE = "Reservation %d already has a report";

    public ReportAlreadyExistsException(Long reservationId) {
        super(CODE, MESSAGE.formatted(reservationId));
    }
}
