package com.geopslabs.geops.reservation.domain.models.commands;

import java.util.List;

/**
 * CreateManyReservationsCommand
 *
 * Command record for creating multiple reservations in a single operation.
 * This command is designed to support bulk reservation creation from the frontend
 * createMany method that expects a bulk endpoint.
 *
 * @summary Command to create multiple reservations at once
 * @param reservations List of CreateReservationCommand objects to create
 *
 * @since 1.0
 * @author GeOps Labs
 */
public record CreateManyReservationsCommand(
    List<CreateReservationCommand> reservations
) {
    /**
     * Compact constructor that validates the command parameters
     *
     * @throws IllegalArgumentException if validation fails
     */
    public CreateManyReservationsCommand {
        if (reservations == null || reservations.isEmpty()) {
            throw new IllegalArgumentException("reservations list cannot be null or empty");
        }

        if (reservations.size() > 100) {
            throw new IllegalArgumentException("cannot create more than 100 reservations at once");
        }
    }
}
