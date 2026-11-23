package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.DomainException;

public class InvalidRegularPriceException extends DomainException {
    public static final String CODE = "INVALID_REGULAR_PRICE";
    private static final String MESSAGE = "El precio normal de la oferta «%s» debe ser mayor que su precio.";

    public InvalidRegularPriceException(String title) {
        super(CODE, MESSAGE.formatted(title));
    }
}
