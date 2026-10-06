package com.geopslabs.geops.identity.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RucTest {

    @ParameterizedTest
    @ValueSource(strings = {"10456789019", "20123456789", " 20123456789 "})
    void acceptsElevenDigits(String number) {
        assertThat(new Ruc(number).isWellFormed()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1045678901", "104567890190", "1045678901A", "10-45678901"})
    void rejectsAnythingElse(String number) {
        assertThat(new Ruc(number).isWellFormed()).isFalse();
    }

    @Test
    void stripsSurroundingSpaces() {
        assertThat(new Ruc(" 10456789019 ").number()).isEqualTo("10456789019");
    }
}
