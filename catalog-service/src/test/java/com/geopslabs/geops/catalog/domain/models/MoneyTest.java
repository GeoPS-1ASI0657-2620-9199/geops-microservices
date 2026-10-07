package com.geopslabs.geops.catalog.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {
    private static final BigDecimal AMOUNT = new BigDecimal("12.50");

    @Test
    void solesUseThePeruvianCurrency() {
        assertThat(Money.soles(AMOUNT).currency()).isEqualTo("PEN");
    }

    @Test
    void solesKeepTheAmount() {
        assertThat(Money.soles(AMOUNT).amount()).isEqualByComparingTo(AMOUNT);
    }
}
