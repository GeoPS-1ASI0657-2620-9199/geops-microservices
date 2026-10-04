package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;

public class CreateReservationCommandAssembler {

    public static CreateReservationCommand toCommandFromResource(CreateReservationRequest resource) {
        return new CreateReservationCommand(
            resource.consumerId(),
            resource.offerId(),
            resource.code(),
            resource.expiresAt()
        );
    }
}
