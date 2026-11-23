package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.application.usecases.DeclareBaselineCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DeclareBaselineRequest(
        @NotNull @Min(1) @Max(100_000) Integer customersPerWeek,
        @NotNull @DecimalMin("0.01") @Digits(integer = 5, fraction = 2) BigDecimal averageTicket) {

    DeclareBaselineCommand toCommand(Long businessId) {
        return new DeclareBaselineCommand(businessId, customersPerWeek, averageTicket);
    }
}
