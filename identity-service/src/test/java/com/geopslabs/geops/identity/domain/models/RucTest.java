package com.geopslabs.geops.identity.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RucTest {

    @ParameterizedTest
    @ValueSource(strings = {"10456789019", "20123456786", " 20123456786 ", "20100047218", "15123456782",
            "17123456785"})
    void acceptsAValidRucWithItsCheckDigit(String number) {
        assertThat(new Ruc(number).isWellFormed()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1045678901", "104567890190", "1045678901A", "10-45678901", "20123456789",
            "10456789018", "30123456786", "11456789019"})
    void rejectsAnythingElse(String number) {
        assertThat(new Ruc(number).isWellFormed()).isFalse();
    }

    @Test
    void mapsAResultOfTenToZeroAndElevenToOne() {
        assertThat(Ruc.checkDigitOf("2099999999")).isZero();
        assertThat(new Ruc("20999999990").isWellFormed()).isTrue();
        assertThat(new Ruc("20111111112").isWellFormed()).isTrue();
    }

    @Test
    void stripsSurroundingSpaces() {
        assertThat(new Ruc(" 10456789019 ").number()).isEqualTo("10456789019");
    }
}
