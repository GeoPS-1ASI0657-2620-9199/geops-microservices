package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.GoneException;

public class ReservationExpiredException extends GoneException {
    private static final String CODE = "RESERVATION_EXPIRED";
    private static final String MESSAGE = "Reservation %d is past its deadline";

    public ReservationExpiredException(Long reservationId) {
        super(CODE, MESSAGE.formatted(reservationId));
    }
}
