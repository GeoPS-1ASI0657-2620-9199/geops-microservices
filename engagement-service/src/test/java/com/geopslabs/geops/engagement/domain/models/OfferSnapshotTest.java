package com.geopslabs.geops.engagement.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OfferSnapshotTest {
    private static final LocalDate LAST_DAY = LocalDate.parse("2026-10-31");
    private static final LocalDate DAY_AFTER = LocalDate.parse("2026-11-01");
    private static final LocalDate BEFORE_LAST_DAY = LocalDate.parse("2026-10-20");

    @Test
    void isStillValidOnItsLastDay() {
        assertThat(offer(OfferSnapshot.PUBLISHED).isExpired(LAST_DAY)).isFalse();
    }

    @Test
    void isExpiredTheDayAfterItsLastDay() {
        assertThat(offer(OfferSnapshot.PUBLISHED).isExpired(DAY_AFTER)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {OfferSnapshot.EXPIRED, OfferSnapshot.REMOVED})
    void anEndedStatusCountsAsExpiredBeforeTheLastDay(String status) {
        assertThat(offer(status).isExpired(BEFORE_LAST_DAY)).isTrue();
    }

    private static OfferSnapshot offer(String status) {
        return new OfferSnapshot(1L, 1L, "Menú ejecutivo a mitad de precio", LAST_DAY, status);
    }
}
