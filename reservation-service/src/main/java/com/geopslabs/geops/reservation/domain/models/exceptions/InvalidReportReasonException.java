package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.InvalidRequestException;

public class InvalidReportReasonException extends InvalidRequestException {
    private static final String CODE = "INVALID_REQUEST";
    private static final String MESSAGE = "Cuéntanos el motivo en %d a %d caracteres.";

    public InvalidReportReasonException(int min, int max) {
        super(CODE, MESSAGE.formatted(min, max));
    }
}
