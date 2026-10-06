package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ForbiddenException;

public class ReservationAccessDeniedException extends ForbiddenException {
    private static final String CODE = "FORBIDDEN";
    private static final String MESSAGE = "Reservation %d belongs to another account";

    public ReservationAccessDeniedException(Long reservationId) {
        super(CODE, MESSAGE.formatted(reservationId));
    }
}
