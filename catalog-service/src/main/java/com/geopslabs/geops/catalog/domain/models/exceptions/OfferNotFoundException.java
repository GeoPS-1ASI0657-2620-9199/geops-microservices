package com.geopslabs.geops.catalog.domain.models.exceptions;

import com.geopslabs.geops.catalog.shared.domain.NotFoundException;

public class OfferNotFoundException extends NotFoundException {
    private static final String CODE = "OFFER_NOT_FOUND";
    private static final String MESSAGE = "La oferta %d no existe.";

    public OfferNotFoundException(Long offerId) {
        super(CODE, MESSAGE.formatted(offerId));
    }
}
