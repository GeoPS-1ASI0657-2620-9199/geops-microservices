package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;
import com.geopslabs.geops.reservation.domain.models.commands.CreateManyReservationsCommand;

import java.util.List;

/**
 * CreateManyReservationsCommandAssembler
 *
 * Assembler class responsible for converting CreateManyReservationsRequest objects
 * to CreateManyReservationsCommand objects. This transformation follows the DDD pattern
 * of converting interface layer Resources to domain layer commands for bulk operations.
 *
 * @summary Converts CreateManyReservationsRequest to CreateManyReservationsCommand
 * @since 1.0
 * @author GeOps Labs
 */
public class CreateManyReservationsCommandAssembler {

    /**
     * Converts a CreateManyReservationsRequest to a CreateManyReservationsCommand.
     *
     * This method transforms the REST API bulk resource representation into
     * a domain command that can be processed by the domain services for
     * creating multiple reservations in a single operation.
     *
     * @param resource The CreateManyReservationsRequest from the REST API request
     * @return A CreateManyReservationsCommand ready for domain processing
     * @throws IllegalArgumentException if the resource contains invalid data
     */
    public static CreateManyReservationsCommand toCommandFromResource(CreateManyReservationsRequest resource) {
        List<CreateReservationCommand> reservationCommands = resource.reservations().stream()
                .map(CreateReservationCommandAssembler::toCommandFromResource)
                .toList();

        return new CreateManyReservationsCommand(reservationCommands);
    }

    /**
     * Converts a list of CreateReservationRequest to a CreateManyReservationsCommand.
     *
     * This method provides an alternative way to create a bulk command directly
     * from a list of individual reservation resources, which matches the frontend
     * expectation of sending an array of reservation resources to the bulk endpoint.
     *
     * @param resources List of CreateReservationRequest from the REST API request
     * @return A CreateManyReservationsCommand ready for domain processing
     * @throws IllegalArgumentException if the resource list contains invalid data
     */
    public static CreateManyReservationsCommand toCommandFromResourceList(List<CreateReservationRequest> resources) {
        if (resources == null || resources.isEmpty()) {
            throw new IllegalArgumentException("resources list cannot be null or empty");
        }

        List<CreateReservationCommand> reservationCommands = resources.stream()
                .map(CreateReservationCommandAssembler::toCommandFromResource)
                .toList();

        return new CreateManyReservationsCommand(reservationCommands);
    }
}
