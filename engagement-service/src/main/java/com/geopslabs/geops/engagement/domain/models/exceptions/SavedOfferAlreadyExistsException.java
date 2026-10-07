package com.geopslabs.geops.engagement.domain.models.exceptions;

import com.geopslabs.geops.engagement.shared.domain.ConflictException;

public class SavedOfferAlreadyExistsException extends ConflictException {
    private static final String CODE = "SAVED_OFFER_ALREADY_EXISTS";
    private static final String MESSAGE = "Ya guardaste esa oferta.";

    public SavedOfferAlreadyExistsException() {
        super(CODE, MESSAGE);
    }
}
