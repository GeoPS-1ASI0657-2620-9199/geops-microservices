package com.geopslabs.geops.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Optional body: a corrected address replaces the declared one in the same attempt (E2-11). It is saved even when
 * the attempt is not approved, so the owner does not type it again.
 */
public record RucVerificationRequest(
        @Schema(description = "Corrected address of the business; omit it to check the declared one",
                example = "Av. José Larco 345, Miraflores", maxLength = 255)
        @Size(max = 255)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
        String address) {
}
