package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.ModerationAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemoveOfferRequest(
        @Schema(description = "Why the offer breaks the publication rules, from 10 to 255 characters",
                example = "La oferta anuncia un precio que el local no respeta.")
        @NotBlank
        @Size(min = ModerationAction.MIN_REASON_LENGTH, max = ModerationAction.MAX_REASON_LENGTH)
        String reason) {
}
