package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.InvalidRequestException;

public class InvalidReportAnswerException extends InvalidRequestException {
    private static final String CODE = "INVALID_REQUEST";
    private static final String MESSAGE = "Escribe tu respuesta en %d a %d caracteres.";

    public InvalidReportAnswerException(int min, int max) {
        super(CODE, MESSAGE.formatted(min, max));
    }
}
