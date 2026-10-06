package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.ConflictException;

public class RucAlreadyRegisteredException extends ConflictException {
    private static final String CODE = "RUC_ALREADY_REGISTERED";
    private static final String MESSAGE = "Ese RUC ya tiene un negocio registrado.";

    public RucAlreadyRegisteredException() {
        super(CODE, MESSAGE);
    }
}
