package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.CreateCampaignCommand;
import com.geopslabs.geops.catalog.application.usecases.CreateOfferCommand;
import com.geopslabs.geops.catalog.domain.models.GeoPoint;
import com.geopslabs.geops.catalog.domain.models.ZoneType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record CreateCampaignRequest(
        @NotBlank @Size(max = CreateCampaignRequest.NAME_LENGTH) String businessName,
        @NotBlank @Size(max = CreateCampaignRequest.NAME_LENGTH) String name,
        @NotBlank String description,
        @NotNull @Valid PeriodRequest period,
        @Valid MoneyRequest estimatedBudget,
        @NotNull @Valid LocationRequest storeLocation,
        @NotNull @Valid ZoneRequest zone,
        @NotEmpty List<@NotNull @Valid OfferRequest> offers) {
    static final int NAME_LENGTH = 150;
    static final int ADDRESS_LENGTH = 255;
    static final int DISTRICT_LENGTH = 100;
    static final int TITLE_LENGTH = 255;
    static final int CATEGORY_LENGTH = 100;
    static final int IMAGE_URL_LENGTH = 500;
    static final String MIN_LATITUDE = "-90";
    static final String MAX_LATITUDE = "90";
    static final String MIN_LONGITUDE = "-180";
    static final String MAX_LONGITUDE = "180";

    public CreateCampaignCommand toCommand(Long businessId) {
        var budget = Optional.ofNullable(estimatedBudget).map(MoneyRequest::amount).orElse(null);
        var zoneCenter = Optional.ofNullable(zone.center()).map(PointRequest::toGeoPoint).orElse(null);
        var offerCommands = offers.stream().map(OfferRequest::toCommand).toList();
        return new CreateCampaignCommand(businessId, businessName, name, description, period.start(), period.end(),
                budget, storeLocation.address(), storeLocation.toGeoPoint(), zone.type(), zoneCenter,
                zone.radiusMeters(), zone.district(), offerCommands);
    }

    public record PeriodRequest(@NotNull LocalDate start, @NotNull LocalDate end) {
    }

    public record MoneyRequest(@NotNull @PositiveOrZero BigDecimal amount, String currency) {
    }

    public record LocationRequest(
            @NotBlank @Size(max = ADDRESS_LENGTH) String address,
            @NotNull @DecimalMin(MIN_LATITUDE) @DecimalMax(MAX_LATITUDE) Double latitude,
            @NotNull @DecimalMin(MIN_LONGITUDE) @DecimalMax(MAX_LONGITUDE) Double longitude) {

        GeoPoint toGeoPoint() {
            return new GeoPoint(latitude, longitude);
        }
    }

    public record PointRequest(
            @NotNull @DecimalMin(MIN_LATITUDE) @DecimalMax(MAX_LATITUDE) Double latitude,
            @NotNull @DecimalMin(MIN_LONGITUDE) @DecimalMax(MAX_LONGITUDE) Double longitude) {

        GeoPoint toGeoPoint() {
            return new GeoPoint(latitude, longitude);
        }
    }

    public record ZoneRequest(
            @NotNull ZoneType type,
            @Valid PointRequest center,
            Integer radiusMeters,
            @Size(max = DISTRICT_LENGTH) String district) {
    }

    public record OfferRequest(
            @NotBlank @Size(max = TITLE_LENGTH) String title,
            @NotBlank String conditions,
            @NotNull @Positive BigDecimal price,
            @NotNull LocalDate validTo,
            @NotBlank @Size(max = CATEGORY_LENGTH) String category,
            @Size(max = IMAGE_URL_LENGTH) String imageUrl) {

        CreateOfferCommand toCommand() {
            return new CreateOfferCommand(title, conditions, price, validTo, category, imageUrl);
        }
    }
}
