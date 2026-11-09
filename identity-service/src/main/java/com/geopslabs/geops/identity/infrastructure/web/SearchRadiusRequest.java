package com.geopslabs.geops.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * The range (5 to 20) is a rule of the domain and answers RADIUS_OUT_OF_RANGE; here only its presence is checked.
 */
public record SearchRadiusRequest(
        @Schema(description = "Walking minutes, from 5 to 20", example = "15", minimum = "5", maximum = "20")
        @NotNull
        Integer radiusMinutes) {
}
