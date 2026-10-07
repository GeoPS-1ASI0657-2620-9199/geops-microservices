package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;

public interface CreateReservationUseCase {
    ReservationResult create(CreateReservationCommand command);
}
