package com.geopslabs.geops.catalog.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The four counters of a campaign (US11): impressions (an offer shown in the nearby search), clicks (its detail
 * opened), reservations and redemptions. The rates are percentages with one decimal, and null when nothing happened
 * before the step: 0 clicks out of 0 impressions is not a 0 % rate.
 */
public record MetricCounts(long impressions, long clicks, long reservations, long redemptions) {
    public static final MetricCounts ZERO = new MetricCounts(0, 0, 0, 0);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int RATE_SCALE = 1;
    private static final int DIVISION_SCALE = 6;

    public MetricCounts plus(MetricCounts other) {
        return new MetricCounts(impressions + other.impressions, clicks + other.clicks,
                reservations + other.reservations, redemptions + other.redemptions);
    }

    /** Clicks out of impressions. */
    public BigDecimal clickThroughRate() {
        return percent(clicks, impressions);
    }

    /** Reservations out of clicks. */
    public BigDecimal reservationRate() {
        return percent(reservations, clicks);
    }

    /** Redemptions out of reservations. */
    public BigDecimal redemptionRate() {
        return percent(redemptions, reservations);
    }

    /** Redemptions out of clicks: of the people who opened an offer, how many came to the store. */
    public BigDecimal conversionRate() {
        return percent(redemptions, clicks);
    }

    static BigDecimal percent(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return BigDecimal.valueOf(part).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(whole), DIVISION_SCALE, RoundingMode.HALF_UP)
                .setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }
}
