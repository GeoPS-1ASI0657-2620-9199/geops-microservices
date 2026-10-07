package com.geopslabs.geops.engagement.infrastructure.web;

import com.geopslabs.geops.engagement.domain.models.commands.CreateReviewCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull @Positive Long businessId,
        @NotNull @Min(CreateReviewRequest.MIN_RATING) @Max(CreateReviewRequest.MAX_RATING) Integer rating,
        @NotBlank @Size(max = CreateReviewRequest.MAX_TEXT_LENGTH) String text) {
    static final long MIN_RATING = 1;
    static final long MAX_RATING = 5;
    static final int MAX_TEXT_LENGTH = 2000;

    public CreateReviewCommand toCommand(Long consumerId) {
        return new CreateReviewCommand(consumerId, businessId, rating, text);
    }
}
