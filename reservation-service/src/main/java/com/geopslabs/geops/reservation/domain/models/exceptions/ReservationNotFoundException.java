package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.NotFoundException;

public class ReservationNotFoundException extends NotFoundException {
    private static final String CODE = "RESERVATION_NOT_FOUND";
    private static final String BY_ID_MESSAGE = "Reservation %d was not found";
    private static final String BY_CODE_MESSAGE = "Reservation with code %s was not found";

    private ReservationNotFoundException(String message) {
        super(CODE, message);
    }

    public static ReservationNotFoundException withId(Long reservationId) {
        return new ReservationNotFoundException(BY_ID_MESSAGE.formatted(reservationId));
    }

    public static ReservationNotFoundException withCode(String code) {
        return new ReservationNotFoundException(BY_CODE_MESSAGE.formatted(code));
    }
}
