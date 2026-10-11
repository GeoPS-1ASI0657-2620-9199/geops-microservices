package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;

public class ReservationReportedException extends ConflictException {
    private static final String CODE = "RESERVATION_REPORTED";
    private static final String MESSAGE = "Reservation %d was reported by the consumer";

    public ReservationReportedException(Long reservationId) {
        super(CODE, MESSAGE.formatted(reservationId));
    }
}
