package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidModerationReasonException extends DomainException {
    public static final String CODE = "INVALID_MODERATION_REASON";
    private static final String MESSAGE = "El motivo del retiro debe tener entre %d y %d caracteres.";

    public InvalidModerationReasonException(int minLength, int maxLength) {
        super(CODE, MESSAGE.formatted(minLength, maxLength));
    }
}
