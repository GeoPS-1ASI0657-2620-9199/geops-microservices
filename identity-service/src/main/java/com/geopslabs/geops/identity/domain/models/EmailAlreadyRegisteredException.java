package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {
    private static final String CODE = "EMAIL_ALREADY_REGISTERED";
    private static final String MESSAGE = "Ese correo ya tiene una cuenta. Inicia sesión o usa otro correo.";

    public EmailAlreadyRegisteredException() {
        super(CODE, MESSAGE);
    }
}
