package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.UnauthenticatedException;

public class InvalidTokenException extends UnauthenticatedException {
    private static final String CODE = "INVALID_TOKEN";
    private static final String MESSAGE = "Tu sesión no es válida o ya terminó. Inicia sesión de nuevo.";

    public InvalidTokenException() {
        super(CODE, MESSAGE);
    }
}
