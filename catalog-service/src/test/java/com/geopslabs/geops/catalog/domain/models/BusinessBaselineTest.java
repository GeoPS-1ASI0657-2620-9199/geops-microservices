package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidBaselineException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessBaselineTest {
    private static final Instant DECLARED_AT = Instant.parse("2026-10-01T15:20:00Z");

    @Test
    void comparesTheRedemptionsWithTheCustomersOfTheSameNumberOfDays() {
        var baseline = new BusinessBaseline(84L, 140, new BigDecimal("18.5"), DECLARED_AT);
        var threeDays = new MetricsPeriod(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-07"));

        var comparison = baseline.compareWith(new MetricCounts(640, 48, 12, 9), threeDays);

        assertThat(comparison.expectedCustomers()).isEqualByComparingTo(new BigDecimal("60.0"));
        assertThat(comparison.campaignCustomers()).isEqualTo(9);
        assertThat(comparison.customersChangePercent()).isEqualByComparingTo(new BigDecimal("15.0"));
        assertThat(comparison.estimatedExtraSales()).isEqualByComparingTo(new BigDecimal("166.50"));
    }

    @Test
    void withoutRedemptionsTheChangeIsZero() {
        var baseline = new BusinessBaseline(84L, 70, new BigDecimal("12.00"), DECLARED_AT);
        var week = new MetricsPeriod(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-07"));

        assertThat(baseline.compareWith(MetricCounts.ZERO, week).customersChangePercent())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void keepsTheTicketWithTwoDecimals() {
        assertThat(new BusinessBaseline(84L, 1, new BigDecimal("18.5"), DECLARED_AT).averageTicket())
                .hasToString("18.50");
    }

    @Test
    void rejectsCustomersOutOfRangeAndATicketThatIsNotAPositiveAmount() {
        assertThatThrownBy(() -> new BusinessBaseline(84L, 0, BigDecimal.TEN, DECLARED_AT))
                .isInstanceOf(InvalidBaselineException.class);
        assertThatThrownBy(() -> new BusinessBaseline(84L, 100_001, BigDecimal.TEN, DECLARED_AT))
                .isInstanceOf(InvalidBaselineException.class);
        assertThatThrownBy(() -> new BusinessBaseline(84L, 10, BigDecimal.ZERO, DECLARED_AT))
                .isInstanceOf(InvalidBaselineException.class);
        assertThatThrownBy(() -> new BusinessBaseline(84L, 10, new BigDecimal("12.345"), DECLARED_AT))
                .isInstanceOf(InvalidBaselineException.class);
        assertThatThrownBy(() -> new BusinessBaseline(84L, 10, new BigDecimal("100000"), DECLARED_AT))
                .isInstanceOf(InvalidBaselineException.class);
    }
}
