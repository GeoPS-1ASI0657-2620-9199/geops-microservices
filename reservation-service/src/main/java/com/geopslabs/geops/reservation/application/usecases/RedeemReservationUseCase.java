package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.RedeemReservationCommand;

public interface RedeemReservationUseCase {
    Reservation redeem(RedeemReservationCommand command);
}
