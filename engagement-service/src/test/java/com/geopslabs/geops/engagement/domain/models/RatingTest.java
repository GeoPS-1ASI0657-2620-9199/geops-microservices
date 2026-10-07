package com.geopslabs.geops.engagement.domain.models;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingTest {

    @ParameterizedTest
    @ValueSource(ints = {Rating.MIN_STARS, Rating.MAX_STARS})
    void acceptsTheEdgesOfTheRange(int stars) {
        assertThat(new Rating(stars).stars()).isEqualTo(stars);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {Rating.MIN_STARS - 1, Rating.MAX_STARS + 1})
    void rejectsAValueOutsideTheRange(Integer stars) {
        assertThatThrownBy(() -> new Rating(stars)).isInstanceOf(IllegalArgumentException.class);
    }
}
