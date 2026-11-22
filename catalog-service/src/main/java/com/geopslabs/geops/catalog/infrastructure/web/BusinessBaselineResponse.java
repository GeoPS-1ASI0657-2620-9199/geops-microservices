package com.geopslabs.geops.catalog.infrastructure.web;

import com.geopslabs.geops.catalog.domain.models.BusinessBaseline;

import java.math.BigDecimal;
import java.time.Instant;

public record BusinessBaselineResponse(Long businessId, int customersPerWeek, BigDecimal averageTicket,
                                       Instant declaredAt) {
    static BusinessBaselineResponse of(BusinessBaseline baseline) {
        return new BusinessBaselineResponse(baseline.businessId(), baseline.customersPerWeek(),
                baseline.averageTicket(), baseline.declaredAt());
    }
}
