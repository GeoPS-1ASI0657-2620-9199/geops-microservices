package com.geopslabs.geops.catalog.application.services;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidPageRequestException extends DomainException {
    public static final String CODE = "INVALID_PAGE";
    private static final String MESSAGE = "page debe ser 0 o mayor y size estar entre 1 y %d";

    public InvalidPageRequestException(int maxPageSize) {
        super(CODE, MESSAGE.formatted(maxPageSize));
    }
}
