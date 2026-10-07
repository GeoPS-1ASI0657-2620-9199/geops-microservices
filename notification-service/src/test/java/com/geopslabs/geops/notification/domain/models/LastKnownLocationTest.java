package com.geopslabs.geops.notification.domain.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LastKnownLocationTest {
    private static final LocalDateTime CAPTURED_AT = LocalDateTime.parse("2026-10-08T12:00:00");
    private static final long FIFTY_NINE_MINUTES = 59;
    private static final long SIXTY_MINUTES = 60;

    @Test
    void isFreshFiftyNineMinutesAfterTheCapture() {
        assertThat(location(CAPTURED_AT).isFresh(CAPTURED_AT.plusMinutes(FIFTY_NINE_MINUTES))).isTrue();
    }

    @Test
    void isNoLongerFreshSixtyMinutesAfterTheCapture() {
        assertThat(location(CAPTURED_AT).isFresh(CAPTURED_AT.plusMinutes(SIXTY_MINUTES))).isFalse();
    }

    @Test
    void aLaterCaptureIsNewer() {
        assertThat(location(CAPTURED_AT.plusMinutes(1)).isNewerThan(location(CAPTURED_AT))).isTrue();
        assertThat(location(CAPTURED_AT).isNewerThan(location(CAPTURED_AT.plusMinutes(1)))).isFalse();
    }

    private static LastKnownLocation location(LocalDateTime capturedAt) {
        return new LastKnownLocation(new GeoPoint(-12.1211, -77.0297), 25, 800, capturedAt);
    }
}
