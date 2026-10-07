package com.geopslabs.geops.reservation.infrastructure.web;

import com.geopslabs.geops.reservation.domain.models.commands.CreateReservationCommand;

public final class CreateReservationCommandAssembler {

    private CreateReservationCommandAssembler() {
    }

    public static CreateReservationCommand toCommand(AuthenticatedUser user, CreateReservationRequest request) {
        return new CreateReservationCommand(user.consumerId(), request.offerId());
    }
}
