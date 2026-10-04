package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;

public class ReservationCodeAlreadyExistsException extends ConflictException {
    private static final String CODE = "RESERVATION_CODE_ALREADY_EXISTS";
    private static final String MESSAGE = "Reservation code %s already exists";

    public ReservationCodeAlreadyExistsException(String code) {
        super(CODE, MESSAGE.formatted(code));
    }
}
