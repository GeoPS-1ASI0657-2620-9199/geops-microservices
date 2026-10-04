package com.geopslabs.geops.reservation.application.services;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.CreateManyReservationsCommand;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateReservationCommand;
import com.geopslabs.geops.reservation.application.usecases.ReservationCommandUseCase;
import com.geopslabs.geops.reservation.domain.ports.ReservationRepositoryPort;
import com.geopslabs.geops.backend.identity.infrastructure.persistence.jpa.UserRepository;
import com.geopslabs.geops.backend.payments.infrastructure.persistence.jpa.PaymentRepository;
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
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Constructor for dependency injection
     *
     * @param reservationRepository The repository for reservation data access
     * @param userRepository The repository for user data access
     * @param paymentRepository The repository for payment data access
     */
    public ReservationCommandService(ReservationRepositoryPort reservationRepository,
                                    UserRepository userRepository,
                                    PaymentRepository paymentRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
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

            // Fetch user and payment entities
            var userOptional = userRepository.findById(command.userId());
            var paymentOptional = paymentRepository.findById(command.paymentId());

            if (userOptional.isEmpty()) {
                throw new IllegalArgumentException("User not found: " + command.userId());
            }
            if (paymentOptional.isEmpty()) {
                throw new IllegalArgumentException("Payment not found: " + command.paymentId());
            }

            // Create new reservation from command with entities
            var reservation = new Reservation(command, userOptional.get(), paymentOptional.get());

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
                        // Fetch user and payment entities
                        var userOptional = userRepository.findById(reservationCommand.userId());
                        var paymentOptional = paymentRepository.findById(reservationCommand.paymentId());

                        if (userOptional.isPresent() && paymentOptional.isPresent()) {
                            var reservation = new Reservation(reservationCommand, userOptional.get(), paymentOptional.get());
                            var savedReservation = reservationRepository.save(reservation);
                            createdReservations.add(savedReservation);
                        } else {
                            System.err.println("User or Payment not found for reservation: " + reservationCommand.code());
                        }
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

