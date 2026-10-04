package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.CreateManyReservationsCommand;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateReservationCommand;
import com.geopslabs.geops.reservation.application.usecases.ReservationCommandUseCase;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ReservationCommandService
 *
 * Implementation of the ReservationCommandUseCase that handles all command operations
 * for reservation management. This service implements the business logic for
 * creating, updating, and managing reservations following DDD principles.
 *
 * @summary Implementation of reservation command service operations
 * @since 1.0
 * @author GeOps Labs
 */
@Transactional
public class ReservationCommandService implements ReservationCommandUseCase {

    private final ReservationRepositoryPort reservationRepository;

    /**
     * Constructor for dependency injection
     *
     * @param reservationRepository The repository for reservation data access
     */
    public ReservationCommandService(ReservationRepositoryPort reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Reservation> handle(CreateReservationCommand command) {
        try {
            // Check if reservation code already exists to ensure uniqueness
            if (reservationRepository.existsByCode(command.code())) {
                throw new IllegalArgumentException("Reservation code already exists: " + command.code());
            }

            var reservation = new Reservation(command);

            // Save the reservation to the repository
            var savedReservation = reservationRepository.save(reservation);

            return Optional.of(savedReservation);

        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error creating reservation: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Reservation> handle(CreateManyReservationsCommand command) {
        List<Reservation> createdReservations = new ArrayList<>();

        try {
            // Process each reservation creation command
            for (CreateReservationCommand reservationCommand : command.reservations()) {
                try {
                    // Check if reservation code already exists
                    if (!reservationRepository.existsByCode(reservationCommand.code())) {
                        var reservation = new Reservation(reservationCommand);
                        var savedReservation = reservationRepository.save(reservation);
                        createdReservations.add(savedReservation);
                    } else {
                        // Log duplicate code warning but continue processing
                        System.err.println("Skipping duplicate reservation code: " + reservationCommand.code());
                    }
                } catch (Exception e) {
                    // Log individual reservation creation error but continue with others
                    System.err.println("Error creating individual reservation: " + e.getMessage());
                }
            }

            return createdReservations;

        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error creating multiple reservations: " + e.getMessage());
            return createdReservations; // Return partial results
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Reservation> handle(UpdateReservationCommand command) {
        try {
            // Find the existing reservation by ID
            var existingReservationOpt = reservationRepository.findById(command.reservationId());

            if (existingReservationOpt.isEmpty()) {
                return Optional.empty();
            }

            var existingReservation = existingReservationOpt.get();

            // Check if new code is unique (if being updated)
            if (command.code() != null && !command.code().equals(existingReservation.getCode())) {
                if (reservationRepository.existsByCode(command.code())) {
                    throw new IllegalArgumentException("Reservation code already exists: " + command.code());
                }
            }

            // Update the reservation with new data
            existingReservation.updateReservation(command);

            // Save the updated reservation
            var updatedReservation = reservationRepository.save(existingReservation);

            return Optional.of(updatedReservation);

        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error updating reservation: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteReservation(Long reservationId) {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("reservationId cannot be null or negative");
        }

        try {
            // First check if reservation exists
            var existingReservationOpt = reservationRepository.findById(reservationId);

            if (existingReservationOpt.isEmpty()) {
                return false;
            }

            // Delete the reservation
            reservationRepository.deleteById(reservationId);

            return true;

        } catch (Exception e) {
            // Log the error (in a real application, use proper logging framework)
            System.err.println("Error deleting reservation: " + e.getMessage());
            return false;
        }
    }
}

