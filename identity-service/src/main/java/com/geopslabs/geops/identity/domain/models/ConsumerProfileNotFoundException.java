package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.NotFoundException;

public class ConsumerProfileNotFoundException extends NotFoundException {
    private static final String CODE = "CONSUMER_PROFILE_NOT_FOUND";
    private static final String MESSAGE = "No encontramos tu perfil de consumidor.";

    public ConsumerProfileNotFoundException() {
        super(CODE, MESSAGE);
    }
}
