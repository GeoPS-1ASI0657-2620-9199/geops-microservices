package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;

public class ReservationNotReportableException extends ConflictException {
    private static final String CODE = "RESERVATION_NOT_REPORTABLE";
    private static final String MESSAGE = "Reservation %d can no longer be reported";

    public ReservationNotReportableException(Long reservationId) {
        super(CODE, MESSAGE.formatted(reservationId));
    }
}
