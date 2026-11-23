package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationJpaAdapterTest {

    @Test
    void withoutTextThePatternMatchesAnything() {
        assertThat(ModerationJpaAdapter.patternOf(null)).isEqualTo("%");
    }

    @Test
    void theTextIsSearchedIgnoringCase() {
        assertThat(ModerationJpaAdapter.patternOf("Almuerzo")).isEqualTo("%almuerzo%");
    }

    @Test
    void wildcardsTypedByTheAdministratorAreLiteral() {
        assertThat(ModerationJpaAdapter.patternOf("50%_off!")).isEqualTo("%50!%!_off!!%");
    }
}
