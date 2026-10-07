package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;

public class OfferNotAvailableException extends ConflictException {
    private static final String CODE = "OFFER_NOT_AVAILABLE";
    private static final String MESSAGE = "Offer %d is no longer valid";

    public OfferNotAvailableException(Long offerId) {
        super(CODE, MESSAGE.formatted(offerId));
    }
}
