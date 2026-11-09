package com.geopslabs.geops.identity.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumerProfileTest {

    @Test
    void startsWithTenMinutes() {
        assertThat(ConsumerProfile.createFor(41L).getSearchRadiusMinutes()).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 15, 20})
    void savesARadiusFromFiveToTwentyMinutes(int minutes) {
        var profile = ConsumerProfile.createFor(41L);

        assertThat(profile.changeSearchRadius(minutes)).isTrue();
        assertThat(profile.getSearchRadiusMinutes()).isEqualTo(minutes);
    }

    @Test
    void savingTheSameRadiusIsNotAChange() {
        var profile = ConsumerProfile.createFor(41L);

        assertThat(profile.changeSearchRadius(10)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 21, 25, 0, -5})
    void rejectsARadiusOutsideTheRange(int minutes) {
        var profile = ConsumerProfile.createFor(41L);

        assertThatThrownBy(() -> profile.changeSearchRadius(minutes))
                .isInstanceOf(SearchRadiusOutOfRangeException.class)
                .hasMessage("Elige un radio entre 5 y 20 minutos a pie.");
        assertThat(profile.getSearchRadiusMinutes()).isEqualTo(10);
    }
}
