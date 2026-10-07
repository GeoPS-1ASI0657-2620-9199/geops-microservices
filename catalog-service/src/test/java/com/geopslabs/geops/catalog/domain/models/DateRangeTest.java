package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidCampaignPeriodException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {
    private static final LocalDate START = LocalDate.parse("2026-10-05");
    private static final LocalDate END = LocalDate.parse("2026-10-31");

    @Test
    void periodEndingBeforeItStartsIsRejected() {
        assertThatThrownBy(() -> new DateRange(END, START)).isInstanceOf(InvalidCampaignPeriodException.class);
    }

    @Test
    void oneDayPeriodIsAccepted() {
        assertThat(new DateRange(START, START).contains(START)).isTrue();
    }

    @Test
    void periodEndedBeforeTodayWhenItsLastDayIsPast() {
        assertThat(new DateRange(START, END).hasEndedBefore(END.plusDays(1))).isTrue();
        assertThat(new DateRange(START, END).hasEndedBefore(END)).isFalse();
    }

    @Test
    void containsBothEdgesAndNothingOutside() {
        var period = new DateRange(START, END);

        assertThat(period.contains(START)).isTrue();
        assertThat(period.contains(END)).isTrue();
        assertThat(period.contains(START.minusDays(1))).isFalse();
        assertThat(period.contains(END.plusDays(1))).isFalse();
    }
}
