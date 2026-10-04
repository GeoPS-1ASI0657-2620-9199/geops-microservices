package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateReservationCommand;
import com.geopslabs.geops.reservation.application.usecases.ReservationCommandUseCase;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class ReservationCommandService implements ReservationCommandUseCase {

    private final ReservationRepositoryPort reservationRepository;

    public ReservationCommandService(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Optional<Reservation> handle(CreateReservationCommand command) {
        try {
            if (reservationRepository.existsByCode(command.code())) {
                throw new IllegalArgumentException("Reservation code already exists: " + command.code());
            }

            var reservation = new Reservation(command);

            var savedReservation = reservationRepository.save(reservation);

            return Optional.of(savedReservation);

        } catch (Exception e) {
            System.err.println("Error creating reservation: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Reservation> handle(UpdateReservationCommand command) {
        try {
            var existingReservationOpt = reservationRepository.findById(command.reservationId());

            if (existingReservationOpt.isEmpty()) {
                return Optional.empty();
            }

            var existingReservation = existingReservationOpt.get();

            if (command.code() != null && !command.code().equals(existingReservation.getCode())) {
                if (reservationRepository.existsByCode(command.code())) {
                    throw new IllegalArgumentException("Reservation code already exists: " + command.code());
                }
            }

            existingReservation.updateReservation(command);

            var updatedReservation = reservationRepository.save(existingReservation);

            return Optional.of(updatedReservation);

        } catch (Exception e) {
            System.err.println("Error updating reservation: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteReservation(Long reservationId) {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("reservationId cannot be null or negative");
        }

        try {
            var existingReservationOpt = reservationRepository.findById(reservationId);

            if (existingReservationOpt.isEmpty()) {
                return false;
            }

            reservationRepository.deleteById(reservationId);

            return true;

        } catch (Exception e) {
            System.err.println("Error deleting reservation: " + e.getMessage());
            return false;
        }
    }
}

