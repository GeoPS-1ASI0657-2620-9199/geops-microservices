package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.ConflictException;

public class PhoneAlreadyRegisteredException extends ConflictException {
    private static final String CODE = "PHONE_ALREADY_REGISTERED";
    private static final String MESSAGE = "Ese teléfono ya tiene una cuenta. Inicia sesión o usa otro teléfono.";

    public PhoneAlreadyRegisteredException() {
        super(CODE, MESSAGE);
    }
}
