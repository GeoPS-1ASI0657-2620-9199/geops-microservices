package com.geopslabs.geops.reservation.domain.models.exceptions;

import com.geopslabs.geops.reservation.shared.domain.ConflictException;

import java.time.LocalDateTime;

public class ReservationAlreadyRedeemedException extends ConflictException {
    private static final String CODE = "RESERVATION_ALREADY_REDEEMED";
    private static final String MESSAGE = "Reservation %d was already redeemed";

    private final LocalDateTime redeemedAt;

    public ReservationAlreadyRedeemedException(Long reservationId, LocalDateTime redeemedAt) {
        super(CODE, MESSAGE.formatted(reservationId));
        this.redeemedAt = redeemedAt;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }
}
