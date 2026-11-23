package com.geopslabs.geops.engagement.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorNameTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Lucía Fernández Ríos | Lucía F.",
            "Rosa Quispe          | Rosa Q.",
            "'  Ana   ñahui'      | Ana Ñ.",
            "Pedro                | Pedro"})
    void keepsTheFirstNameAndTheInitialOfTheNextWord(String fullName, String expected) {
        assertThat(AuthorName.shorten(fullName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void aMissingNameGivesNoName(String fullName) {
        assertThat(AuthorName.shorten(fullName)).isNull();
    }

    @Test
    void neverExceedsTheLengthOfTheColumn() {
        var longFirstName = "A".repeat(AuthorName.MAX_LENGTH * 2);

        assertThat(AuthorName.shorten(longFirstName + " Fernández")).hasSize(AuthorName.MAX_LENGTH);
    }
}
