package com.geopslabs.geops.identity.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoPointTest {
    private static final String INVALID_LOCATION = "INVALID_LOCATION";

    @ParameterizedTest
    @CsvSource({"-90, -180", "90, 180", "0, 0", "-12.0681, -77.0350"})
    void acceptsCoordinatesWithinLimits(double latitude, double longitude) {
        var point = GeoPoint.of(latitude, longitude);

        assertThat(point.latitude()).isEqualTo(latitude);
        assertThat(point.longitude()).isEqualTo(longitude);
    }

    @ParameterizedTest
    @CsvSource({"-90.000001, -77.0350", "90.000001, -77.0350", "95.0, -77.0350"})
    void rejectsLatitudeOutOfRange(double latitude, double longitude) {
        assertThatThrownBy(() -> GeoPoint.of(latitude, longitude))
                .isInstanceOf(InvalidLocationException.class)
                .hasMessage("La latitud debe estar entre -90 y 90. Corrige la ubicación del local.")
                .extracting("code").isEqualTo(INVALID_LOCATION);
    }

    @ParameterizedTest
    @CsvSource({"-12.0681, -180.000001", "-12.0681, 180.000001", "-12.0681, -200.0"})
    void rejectsLongitudeOutOfRange(double latitude, double longitude) {
        assertThatThrownBy(() -> GeoPoint.of(latitude, longitude))
                .isInstanceOf(InvalidLocationException.class)
                .hasMessage("La longitud debe estar entre -180 y 180. Corrige la ubicación del local.");
    }

    @Test
    void rejectsNotANumber() {
        assertThatThrownBy(() -> GeoPoint.of(Double.NaN, -77.0350)).isInstanceOf(InvalidLocationException.class);
    }

    @Test
    void rejectsMissingCoordinates() {
        assertThatThrownBy(() -> GeoPoint.of(null, -77.0350))
                .isInstanceOf(InvalidLocationException.class)
                .hasMessageContaining("Falta la latitud o la longitud");
        assertThatThrownBy(() -> GeoPoint.of(-12.0681, null)).isInstanceOf(InvalidLocationException.class);
    }
}
