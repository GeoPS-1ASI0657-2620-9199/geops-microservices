package com.geopslabs.geops.catalog.application.usecases;

import java.math.BigDecimal;

public record DeclareBaselineCommand(Long businessId, int customersPerWeek, BigDecimal averageTicket) {
}
