package com.geopslabs.geops.catalog.domain.models;

import com.geopslabs.geops.catalog.domain.models.exceptions.InvalidSearchRadiusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalkingRadiusTest {

    @Test
    void convertsFiveMinutesToFourHundredMeters() {
        assertThat(new WalkingRadius(5).toMeters()).isEqualTo(400);
    }

    @Test
    void convertsTwentyMinutesToSixteenHundredMeters() {
        assertThat(new WalkingRadius(20).toMeters()).isEqualTo(1_600);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 21})
    void rejectsMinutesOutsideTheRangeWithTheRange(int minutes) {
        assertThatThrownBy(() -> new WalkingRadius(minutes))
                .isInstanceOf(InvalidSearchRadiusException.class)
                .hasMessage("El radio debe estar entre 5 y 20 minutos a pie");
    }

    @Test
    void roundsWalkingMinutesUp() {
        assertThat(WalkingRadius.walkMinutesFor(350)).isEqualTo(5);
        assertThat(WalkingRadius.walkMinutesFor(320)).isEqualTo(4);
    }
}
