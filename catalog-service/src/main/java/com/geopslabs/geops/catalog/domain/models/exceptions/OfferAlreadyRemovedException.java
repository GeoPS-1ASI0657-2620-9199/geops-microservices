package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.ConflictException;

public class OfferAlreadyRemovedException extends ConflictException {
    public static final String CODE = "OFFER_ALREADY_REMOVED";
    private static final String MESSAGE = "La oferta %d ya fue retirada del catálogo.";

    public OfferAlreadyRemovedException(Long offerId) {
        super(CODE, MESSAGE.formatted(offerId));
    }
}
