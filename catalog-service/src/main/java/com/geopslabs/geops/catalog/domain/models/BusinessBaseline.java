package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidBaselineException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * The numbers the business had before GeoPS (US45): customers per week and average ticket, with the moment they
 * were declared. Declaring again replaces them and moves the date.
 */
public record BusinessBaseline(Long businessId, int customersPerWeek, BigDecimal averageTicket, Instant declaredAt) {
    public static final int MAX_CUSTOMERS_PER_WEEK = 100_000;
    private static final BigDecimal MAX_AVERAGE_TICKET = new BigDecimal("100000");
    private static final BigDecimal DAYS_PER_WEEK = BigDecimal.valueOf(7);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MONEY_SCALE = 2;
    private static final int COUNT_SCALE = 1;

    public BusinessBaseline {
        Objects.requireNonNull(businessId);
        Objects.requireNonNull(declaredAt);
        if (customersPerWeek < 1 || customersPerWeek > MAX_CUSTOMERS_PER_WEEK) {
            throw InvalidBaselineException.customersPerWeek(MAX_CUSTOMERS_PER_WEEK);
        }
        if (averageTicket == null || averageTicket.signum() <= 0 || averageTicket.compareTo(MAX_AVERAGE_TICKET) >= 0
                || averageTicket.stripTrailingZeros().scale() > MONEY_SCALE) {
            throw InvalidBaselineException.averageTicket();
        }
        averageTicket = averageTicket.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    public BaselineComparison compareWith(MetricCounts counts, MetricsPeriod period) {
        var expected = BigDecimal.valueOf(customersPerWeek).multiply(BigDecimal.valueOf(period.days()))
                .divide(DAYS_PER_WEEK, COUNT_SCALE, RoundingMode.HALF_UP);
        var change = expected.signum() == 0 ? null
                : BigDecimal.valueOf(counts.redemptions()).multiply(HUNDRED)
                        .divide(expected, COUNT_SCALE, RoundingMode.HALF_UP);
        var extraSales = averageTicket.multiply(BigDecimal.valueOf(counts.redemptions()));
        return new BaselineComparison(expected, counts.redemptions(), change, extraSales);
    }
}
