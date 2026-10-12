package com.geopslabs.geops.reservation.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record RedeemReservationRequest(@NotBlank String code) {
}
