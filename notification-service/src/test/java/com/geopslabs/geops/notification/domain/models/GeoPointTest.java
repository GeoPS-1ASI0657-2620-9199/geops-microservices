package com.geopslabs.geops.notification.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoPointTest {

    @ParameterizedTest
    @CsvSource({"-90, -180", "90, 180", "-12.1211, -77.0297"})
    void acceptsCoordinatesWithinTheRange(double latitude, double longitude) {
        assertThat(new GeoPoint(latitude, longitude).latitude()).isEqualTo(latitude);
    }

    @ParameterizedTest
    @CsvSource({"90.0001, 0", "0, -180.0001"})
    void rejectsCoordinatesOutsideTheRange(double latitude, double longitude) {
        assertThatThrownBy(() -> new GeoPoint(latitude, longitude)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAMissingCoordinate() {
        assertThatThrownBy(() -> new GeoPoint(null, 0.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
