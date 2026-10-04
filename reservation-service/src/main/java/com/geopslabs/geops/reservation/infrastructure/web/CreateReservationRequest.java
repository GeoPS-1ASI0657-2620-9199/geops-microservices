package com.geopslabs.geops.reservation.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(@NotNull @Positive Long offerId) {
}
