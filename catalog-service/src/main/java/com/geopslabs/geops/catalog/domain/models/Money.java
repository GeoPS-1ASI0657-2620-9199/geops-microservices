package com.geopslabs.geops.catalog.domain.models;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) {
    public static final String SOLES = "PEN";

    public static Money soles(BigDecimal amount) {
        return new Money(amount, SOLES);
    }
}
