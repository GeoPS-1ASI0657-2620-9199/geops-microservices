package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.application.usecases.ReservationCommandUseCase;
import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.exceptions.ReservationCodeAlreadyExistsException;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;

public class ReservationCommandService implements ReservationCommandUseCase {
    private final ReservationRepositoryPort reservationRepository;

    public ReservationCommandService(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Reservation handle(CreateReservationCommand command) {
        if (reservationRepository.existsByCode(command.code())) {
            throw new ReservationCodeAlreadyExistsException(command.code());
        }
        return reservationRepository.save(new Reservation(command));
    }
}
