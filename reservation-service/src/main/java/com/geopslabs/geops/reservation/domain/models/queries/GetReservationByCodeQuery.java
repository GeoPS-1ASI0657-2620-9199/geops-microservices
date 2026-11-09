package com.geopslabs.geops.reservation.domain.models.queries;

import com.geopslabs.geops.reservation.domain.models.ReservationCode;

/**
 * The code is normalized as in the redemption, so what the business types finds the same reservation in both.
 */
public record GetReservationByCodeQuery(String code, Long businessId) {

    public GetReservationByCodeQuery {
        code = ReservationCode.normalize(code);
    }
}
