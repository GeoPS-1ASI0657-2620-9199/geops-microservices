package com.geopslabs.geops.notification.infrastructure.web;

import com.geopslabs.geops.notification.domain.models.commands.RecordLocationCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record RecordLocationRequest(
        @NotNull @DecimalMin(RecordLocationRequest.MIN_LATITUDE) @DecimalMax(RecordLocationRequest.MAX_LATITUDE)
        Double latitude,
        @NotNull @DecimalMin(RecordLocationRequest.MIN_LONGITUDE) @DecimalMax(RecordLocationRequest.MAX_LONGITUDE)
        Double longitude,
        @NotNull @Positive Integer accuracyMeters,
        @NotNull Instant capturedAt) {
    static final String MIN_LATITUDE = "-90";
    static final String MAX_LATITUDE = "90";
    static final String MIN_LONGITUDE = "-180";
    static final String MAX_LONGITUDE = "180";

    public RecordLocationCommand toCommand(Long consumerId) {
        return new RecordLocationCommand(consumerId, latitude, longitude, accuracyMeters, capturedAt);
    }
}
