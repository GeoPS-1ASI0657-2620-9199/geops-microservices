package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.ConflictException;

public class OfferNotAvailableException extends ConflictException {
    private static final String CODE = "OFFER_NOT_AVAILABLE";
    private static final String MESSAGE = "Esa oferta ya no está vigente.";

    public OfferNotAvailableException() {
        super(CODE, MESSAGE);
    }
}
