package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidMetricsPeriodException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricsPeriodTest {
    private static final DateRange OCTOBER = new DateRange(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
    private static final LocalDate TODAY = LocalDate.parse("2026-10-08");

    @Test
    void withoutDatesItIsTheValidityOfTheCampaignUpToToday() {
        var period = MetricsPeriod.resolve(null, null, OCTOBER, TODAY);

        assertThat(period).isEqualTo(new MetricsPeriod(LocalDate.parse("2026-10-01"), TODAY));
        assertThat(period.days()).isEqualTo(8);
    }

    @Test
    void anEndedCampaignStopsAtItsLastDay() {
        var period = MetricsPeriod.resolve(null, null, OCTOBER, LocalDate.parse("2026-11-20"));

        assertThat(period.to()).isEqualTo(LocalDate.parse("2026-10-31"));
    }

    @Test
    void aCampaignThatHasNotStartedShowsItsFirstDay() {
        var period = MetricsPeriod.resolve(null, null, OCTOBER, LocalDate.parse("2026-09-20"));

        assertThat(period).isEqualTo(new MetricsPeriod(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01")));
    }

    @Test
    void theDatesAskedForAreKept() {
        var period = MetricsPeriod.resolve(LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-05"), OCTOBER, TODAY);

        assertThat(period.dates()).containsExactly(LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-04"),
                LocalDate.parse("2026-10-05"));
    }

    @Test
    void rejectsAnEndBeforeTheStartAndMoreThanAYear() {
        assertThatThrownBy(() -> new MetricsPeriod(TODAY, TODAY.minusDays(1)))
                .isInstanceOf(InvalidMetricsPeriodException.class);
        assertThatThrownBy(() -> new MetricsPeriod(TODAY.minusDays(MetricsPeriod.MAX_DAYS), TODAY))
                .isInstanceOf(InvalidMetricsPeriodException.class);
        assertThat(new MetricsPeriod(TODAY.minusDays(MetricsPeriod.MAX_DAYS - 1L), TODAY).days())
                .isEqualTo(MetricsPeriod.MAX_DAYS);
    }

    @Test
    void theLastThirtyDaysEndToday() {
        var period = MetricsPeriod.lastDays(30, TODAY);

        assertThat(period.from()).isEqualTo(LocalDate.parse("2026-09-09"));
        assertThat(period.days()).isEqualTo(30);
    }
}
