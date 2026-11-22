package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidSearchTextException extends DomainException {
    public static final String CODE = "INVALID_REQUEST";
    private static final String MESSAGE = "La búsqueda debe tener entre %d y %d caracteres";

    public InvalidSearchTextException(int minLength, int maxLength) {
        super(CODE, MESSAGE.formatted(minLength, maxLength));
    }
}
