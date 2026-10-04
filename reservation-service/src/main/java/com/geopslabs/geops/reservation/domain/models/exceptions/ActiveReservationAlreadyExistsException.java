package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;

public class ActiveReservationAlreadyExistsException extends ConflictException {
    private static final String CODE = "RESERVATION_ALREADY_ACTIVE";
    private static final String MESSAGE = "Consumer %d already has an active reservation for offer %d";

    public ActiveReservationAlreadyExistsException(Long consumerId, Long offerId) {
        super(CODE, MESSAGE.formatted(consumerId, offerId));
    }
}
