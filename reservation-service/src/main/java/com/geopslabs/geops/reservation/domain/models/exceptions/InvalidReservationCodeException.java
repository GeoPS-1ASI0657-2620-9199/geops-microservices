package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.shared.domain.InvalidRequestException;

public class InvalidReservationCodeException extends InvalidRequestException {
    private static final String CODE = "INVALID_REQUEST";
    private static final String MESSAGE = "code must have %d characters from %s";

    public InvalidReservationCodeException() {
        super(CODE, MESSAGE.formatted(ReservationCode.CODE_LENGTH, ReservationCode.ALPHABET));
    }
}
