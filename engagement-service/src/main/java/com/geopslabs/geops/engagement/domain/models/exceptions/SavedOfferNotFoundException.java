package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.NotFoundException;

public class SavedOfferNotFoundException extends NotFoundException {
    private static final String CODE = "SAVED_OFFER_NOT_FOUND";
    private static final String MESSAGE = "No tienes guardada esa oferta.";

    public SavedOfferNotFoundException() {
        super(CODE, MESSAGE);
    }
}
