package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidMetricsPeriodException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * The days asked for in the metrics (US11), both ends included. Without dates it is the validity of the campaign up
 * to today; a missing end is filled the same way. It covers at most a year, so the daily series stays small.
 */
public record MetricsPeriod(LocalDate from, LocalDate to) {
    public static final int MAX_DAYS = 366;

    public MetricsPeriod {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (to.isBefore(from)) {
            throw InvalidMetricsPeriodException.endBeforeStart();
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw InvalidMetricsPeriodException.tooLong(MAX_DAYS);
        }
    }

    public static MetricsPeriod resolve(LocalDate from, LocalDate to, DateRange campaignValidity, LocalDate today) {
        var start = from != null ? from : campaignValidity.start();
        var end = to;
        if (end == null) {
            end = campaignValidity.end().isBefore(today) ? campaignValidity.end() : today;
            if (end.isBefore(start)) {
                end = start;
            }
        }
        return new MetricsPeriod(start, end);
    }

    /** The last {@code days} days up to today, the default of the summary of every campaign. */
    public static MetricsPeriod lastDays(int days, LocalDate today) {
        return new MetricsPeriod(today.minusDays(days - 1L), today);
    }

    public int days() {
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    public List<LocalDate> dates() {
        return from.datesUntil(to.plusDays(1)).toList();
    }
}
