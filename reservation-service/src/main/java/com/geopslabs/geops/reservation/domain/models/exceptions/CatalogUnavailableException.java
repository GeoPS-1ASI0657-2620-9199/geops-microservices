package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.UnavailableException;

public class CatalogUnavailableException extends UnavailableException {
    private static final String CODE = "CATALOG_UNAVAILABLE";
    private static final String MESSAGE = "El catálogo no responde en este momento. Intenta reservar de nuevo en unos minutos.";

    public CatalogUnavailableException() {
        super(CODE, MESSAGE);
    }
}
