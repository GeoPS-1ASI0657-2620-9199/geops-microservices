package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.NotFoundException;

public class OfferNotFoundException extends NotFoundException {
    private static final String CODE = "OFFER_NOT_FOUND";
    private static final String MESSAGE = "No encontramos esa oferta.";

    public OfferNotFoundException() {
        super(CODE, MESSAGE);
    }
}
