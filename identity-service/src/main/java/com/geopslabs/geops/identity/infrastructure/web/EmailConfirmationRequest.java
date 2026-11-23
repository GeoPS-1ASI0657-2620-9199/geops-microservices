package com.geopslabs.geops.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailConfirmationRequest(
        @Schema(description = "Token of the confirmation link sent by email",
                example = "q3Zt8b1xV0mWc9yRkP2sLhN4dF6gJ7aE5uT0iO3pQ1w")
        @NotBlank @Size(max = 128) String token) {
}
