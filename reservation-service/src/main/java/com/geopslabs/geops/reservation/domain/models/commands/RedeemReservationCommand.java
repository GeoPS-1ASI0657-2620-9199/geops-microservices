package com.geopslabs.geops.reservation.domain.models.commands;

import com.geopslabs.geops.reservation.domain.models.ReservationCode;
import com.geopslabs.geops.reservation.domain.models.exceptions.InvalidReservationCodeException;

/**
 * The code arrives as typed at the store; it is normalized the same way for every lookup.
 */
public record RedeemReservationCommand(String code, Long businessId) {

    public RedeemReservationCommand {
        code = ReservationCode.normalize(code);
        if (!ReservationCode.isWellFormed(code)) {
            throw new InvalidReservationCodeException();
        }
    }
}
