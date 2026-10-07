package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidGeoPointException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeoPointTest {
    private static final GeoPoint MIRAFLORES = new GeoPoint(-12.1211, -77.0297);

    @Test
    void keepsAValidPoint() {
        assertThat(MIRAFLORES.latitude()).isEqualTo(-12.1211);
        assertThat(MIRAFLORES.longitude()).isEqualTo(-77.0297);
    }

    @Test
    void rejectsALatitudeOutOfRangeWithTheReason() {
        assertThatThrownBy(() -> new GeoPoint(95, -77.0297))
                .isInstanceOf(InvalidGeoPointException.class)
                .hasMessage("La latitud debe estar entre -90 y 90 y la longitud entre -180 y 180");
    }

    @Test
    void rejectsALongitudeOutOfRange() {
        assertThatThrownBy(() -> new GeoPoint(-12.1211, -200)).isInstanceOf(InvalidGeoPointException.class);
    }

    @Test
    void acceptsTheLimitsOfEachRange() {
        assertThat(GeoPoint.isValid(-90, 180)).isTrue();
        assertThat(GeoPoint.isValid(90, -180)).isTrue();
    }

    @Test
    void measuresOneHundredthOfADegreeNorthAsTheSphereDoes() {
        var north = new GeoPoint(-12.1111, -77.0297);

        assertThat(MIRAFLORES.distanceTo(north)).isCloseTo(1_111.95, within(0.5));
    }

    @Test
    void measuresZeroToItself() {
        assertThat(MIRAFLORES.distanceTo(MIRAFLORES)).isZero();
    }
}
