package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.LoadPublicOffersCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * E2-09: from 1 to 200 offers per batch.
 */
public record LoadPublicOffersRequest(
        @NotNull @Size(min = 1, max = LoadPublicOffersRequest.MAX_BATCH) List<@NotNull @Valid PublicOfferRequest> offers) {
    static final int MAX_BATCH = 200;
    private static final int TITLE_LENGTH = 255;
    private static final int CATEGORY_LENGTH = 100;
    private static final int ADDRESS_LENGTH = 255;
    private static final int SOURCE_LENGTH = 150;
    private static final int IMAGE_URL_LENGTH = 500;

    LoadPublicOffersCommand toCommand() {
        return new LoadPublicOffersCommand(offers.stream().map(PublicOfferRequest::toCommand).toList());
    }

    public record PublicOfferRequest(
            @NotBlank @Size(max = TITLE_LENGTH) String title,
            @NotBlank String conditions,
            @NotNull @PositiveOrZero BigDecimal price,
            @NotNull LocalDate validTo,
            @NotBlank @Size(max = CATEGORY_LENGTH) String category,
            @NotBlank @Size(max = ADDRESS_LENGTH) String address,
            @NotBlank @Size(max = SOURCE_LENGTH) String sourceName,
            @Size(max = IMAGE_URL_LENGTH) String imageUrl) {

        LoadPublicOffersCommand.PublicOffer toCommand() {
            return new LoadPublicOffersCommand.PublicOffer(title, conditions, price, validTo, category, address,
                    sourceName, imageUrl);
        }
    }
}
