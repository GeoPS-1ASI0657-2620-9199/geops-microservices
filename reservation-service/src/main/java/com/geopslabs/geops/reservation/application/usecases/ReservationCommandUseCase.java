package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateReservationCommand;

import java.util.Optional;

public interface ReservationCommandUseCase {

    Optional<Reservation> handle(CreateReservationCommand command);

    Optional<Reservation> handle(UpdateReservationCommand command);

    boolean deleteReservation(Long reservationId);
}
