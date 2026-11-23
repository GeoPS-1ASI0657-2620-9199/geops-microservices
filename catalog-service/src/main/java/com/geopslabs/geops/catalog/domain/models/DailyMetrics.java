package com.geopslabs.geops.catalog.domain.models;

import java.time.LocalDate;
import java.util.Objects;

/** The counters of one campaign on one day of Lima. */
public record DailyMetrics(LocalDate date, MetricCounts counts) {
    public DailyMetrics {
        Objects.requireNonNull(date);
        Objects.requireNonNull(counts);
    }

    public static DailyMetrics empty(LocalDate date) {
        return new DailyMetrics(date, MetricCounts.ZERO);
    }
}
