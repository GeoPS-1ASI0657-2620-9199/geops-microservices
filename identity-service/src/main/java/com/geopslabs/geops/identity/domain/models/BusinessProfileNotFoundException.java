package com.geopslabs.geops.identity.domain.models;

import com.geopslabs.geops.identity.shared.NotFoundException;

public class BusinessProfileNotFoundException extends NotFoundException {
    private static final String CODE = "BUSINESS_PROFILE_NOT_FOUND";
    private static final String MESSAGE = "No encontramos el perfil de ese negocio.";

    public BusinessProfileNotFoundException() {
        super(CODE, MESSAGE);
    }
}
