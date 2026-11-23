package com.geopslabs.geops.catalog.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MetricCountsTest {

    @Test
    void ratesArePercentagesWithOneDecimal() {
        var counts = new MetricCounts(640, 48, 12, 9);

        assertThat(counts.clickThroughRate()).isEqualByComparingTo(new BigDecimal("7.5"));
        assertThat(counts.reservationRate()).isEqualByComparingTo(new BigDecimal("25.0"));
        assertThat(counts.redemptionRate()).isEqualByComparingTo(new BigDecimal("75.0"));
        assertThat(counts.conversionRate()).isEqualByComparingTo(new BigDecimal("18.8"));
    }

    @Test
    void aRateWithoutBaseIsNullAndNotZero() {
        assertThat(MetricCounts.ZERO.clickThroughRate()).isNull();
        assertThat(new MetricCounts(10, 0, 0, 0).clickThroughRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(new MetricCounts(10, 0, 0, 0).conversionRate()).isNull();
    }

    @Test
    void addsEveryCounter() {
        var total = new MetricCounts(1, 2, 3, 4).plus(new MetricCounts(10, 20, 30, 40));

        assertThat(total).isEqualTo(new MetricCounts(11, 22, 33, 44));
    }
}
