package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.UnavailableException;

public class CatalogUnavailableException extends UnavailableException {
    private static final String CODE = "CATALOG_UNAVAILABLE";
    private static final String MESSAGE = "Catalog is not available";

    public CatalogUnavailableException() {
        super(CODE, MESSAGE);
    }
}
