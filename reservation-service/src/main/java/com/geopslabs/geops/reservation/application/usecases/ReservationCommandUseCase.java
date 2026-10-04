package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;

public interface ReservationCommandUseCase {
    Reservation handle(CreateReservationCommand command);
}
