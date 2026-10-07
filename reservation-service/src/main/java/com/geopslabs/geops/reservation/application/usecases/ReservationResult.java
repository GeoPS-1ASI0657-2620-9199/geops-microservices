package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;

public record ReservationResult(Reservation reservation, boolean created) {

    public static ReservationResult created(Reservation reservation) {
        return new ReservationResult(reservation, true);
    }

    public static ReservationResult existing(Reservation reservation) {
        return new ReservationResult(reservation, false);
    }
}
