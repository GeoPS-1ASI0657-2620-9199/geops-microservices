package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.NotFoundException;

public class OfferNotFoundException extends NotFoundException {
    private static final String CODE = "OFFER_NOT_FOUND";
    private static final String MESSAGE = "Offer %d was not found";

    public OfferNotFoundException(Long offerId) {
        super(CODE, MESSAGE.formatted(offerId));
    }
}
