package com.geopslabs.geops.notification.domain.models.exceptions;

import com.geopslabs.geops.notification.shared.domain.RuleViolationException;

public class InvalidCaptureTimeException extends RuleViolationException {
    private static final String CODE = "INVALID_CAPTURE_TIME";
    private static final String MESSAGE = "La hora de captura no puede estar en el futuro.";

    public InvalidCaptureTimeException() {
        super(CODE, MESSAGE);
    }
}
