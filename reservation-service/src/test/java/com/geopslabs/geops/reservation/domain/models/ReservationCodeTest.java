package com.geopslabs.geops.reservation.domain.models;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationCodeTest {
    private static final int GENERATIONS = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    @RepeatedTest(GENERATIONS)
    void generatesEightCharactersFromTheAlphabet() {
        var code = ReservationCode.generate(RANDOM);

        assertThat(code.value()).hasSize(ReservationCode.CODE_LENGTH);
        assertThat(code.value().chars()).allMatch(character -> ReservationCode.ALPHABET.indexOf(character) >= 0);
    }

    @Test
    void alphabetLeavesOutCharactersThatLookAlike() {
        assertThat(ReservationCode.ALPHABET).doesNotContain("0", "O", "1", "I", "L");
    }

    @Test
    void acceptsAWellFormedCode() {
        assertThat(new ReservationCode("K7P3XM9Q").value()).isEqualTo("K7P3XM9Q");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"K7P3XM9", "K7P3XM9QA", "K7P3XM0Q", "k7p3xm9q", "K7P3-M9Q"})
    void rejectsAValueOfAnotherLengthOrAlphabet(String value) {
        assertThatThrownBy(() -> new ReservationCode(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
