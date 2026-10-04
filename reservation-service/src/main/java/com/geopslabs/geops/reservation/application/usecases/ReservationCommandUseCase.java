package com.geopslabs.geops.reservation.application.usecases;

import com.geopslabs.geops.reservation.domain.models.Reservation;
import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.CreateManyReservationsCommand;
import com.geopslabs.geops.reservation.domain.models.commands.UpdateReservationCommand;

import java.util.List;
import java.util.Optional;

/**
 * ReservationCommandService
 *
 * Domain service interface that defines command operations for reservation management.
 * This service handles all write operations (Create, Update, Delete) following the
 * Command Query Responsibility Segregation (CQRS) pattern.
 *
 * @summary Service interface for handling reservation command operations
 * @since 1.0
 * @author GeOps Labs
 */
public interface ReservationCommandUseCase {

    /**
     * Handles the creation of a new reservation.
     *
     * This method processes the command to create a new reservation, validates the input,
     * and persists the reservation data. It supports reservation generation from payments
     * and offer redemption scenarios.
     *
     * @param command The command containing all necessary data for reservation creation
     * @return An Optional containing the created Reservation if successful, empty if failed
     * @throws IllegalArgumentException if the command contains invalid data
     */
    Optional<Reservation> handle(CreateReservationCommand command);

    /**
     * Handles the creation of multiple reservations in a single operation.
     *
     * This method processes the bulk creation command to create multiple reservations
     * at once, which is useful for batch operations and improved performance.
     * It corresponds to the frontend's createMany method that expects a bulk endpoint.
     *
     * @param command The command containing multiple reservation creation data
     * @return A List containing the created Reservations
     * @throws IllegalArgumentException if the command contains invalid data
     */
    List<Reservation> handle(CreateManyReservationsCommand command);

    /**
     * Handles the update of an existing reservation.
     *
     * This method processes the command to update reservation data such as product type,
     * offer reference, reservation code, and expiration date. It performs partial updates
     * based on provided fields.
     *
     * @param command The command containing the reservation ID and updated data
     * @return An Optional containing the updated Reservation if successful, empty if not found
     * @throws IllegalArgumentException if the command contains invalid data
     */
    Optional<Reservation> handle(UpdateReservationCommand command);

    /**
     * Deletes a reservation by its ID.
     *
     * This method permanently removes a reservation from the system.
     * This operation should be used with caution as it cannot be undone.
     *
     * @param reservationId The unique identifier of the reservation to delete
     * @return true if the reservation was successfully deleted, false if not found
     * @throws IllegalArgumentException if the reservation ID is invalid
     */
    boolean deleteReservation(Long reservationId);
}
