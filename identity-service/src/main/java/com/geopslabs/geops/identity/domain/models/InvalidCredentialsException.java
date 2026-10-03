package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.UnauthenticatedException;

public class InvalidCredentialsException extends UnauthenticatedException {
    private static final String CODE = "INVALID_CREDENTIALS";
    private static final String MESSAGE = "No se pudo iniciar sesión con esos datos.";

    public InvalidCredentialsException() {
        super(CODE, MESSAGE);
    }
}
